import json
import random
import urllib.request
import urllib.error
import uuid
import time
from decimal import Decimal


# =========================================================
# CONFIG
# =========================================================

BASE_URL = "http://localhost:8080"

LOGIN_URL = f"{BASE_URL}/api/auth/login"
SEND_MONEY_URL = f"{BASE_URL}/api/payments/send"

MIN_AMOUNT = Decimal("50.00")
MAX_AMOUNT = Decimal("2000.00")

# Each pair will send money back and forth this many times
ROUNDS_PER_PAIR = 3


# =========================================================
# HTTP
# =========================================================

def request_json(method, url, data=None, token=None):

    headers = {
        "Content-Type": "application/json"
    }

    if token:
        headers["Authorization"] = f"Bearer {token}"

    encoded_data = None

    if data is not None:
        encoded_data = json.dumps(data).encode("utf-8")

    request = urllib.request.Request(
        url,
        data=encoded_data,
        headers=headers,
        method=method
    )

    try:

        with urllib.request.urlopen(request) as response:

            body = response.read().decode("utf-8")

            if body:
                try:
                    return response.status, json.loads(body)
                except json.JSONDecodeError:
                    return response.status, body

            return response.status, {}

    except urllib.error.HTTPError as e:

        body = e.read().decode("utf-8")

        try:
            body = json.loads(body)
        except json.JSONDecodeError:
            pass

        return e.code, body

    except urllib.error.URLError as e:

        return 0, str(e)


# =========================================================
# LOGIN
# =========================================================

def login(user):

    status, response = request_json(
        "POST",
        LOGIN_URL,
        {
            "email": user["email"],
            "password": user["password"]
        }
    )

    if status != 200:

        print(
            f"❌ LOGIN FAILED: "
            f"{user['email']} HTTP {status}"
        )

        print(response)

        return None

    try:

        return response["data"]["accessToken"]

    except (KeyError, TypeError):

        print(
            f"❌ Could not find accessToken "
            f"for {user['email']}"
        )

        print(response)

        return None


# =========================================================
# RANDOM MONEY
# =========================================================

def random_amount():

    min_cents = int(MIN_AMOUNT * 100)
    max_cents = int(MAX_AMOUNT * 100)

    cents = random.randint(
        min_cents,
        max_cents
    )

    return Decimal(cents) / Decimal("100")


# =========================================================
# SEND MONEY
# =========================================================

def send_money(
    sender,
    recipient,
    token,
    amount,
    direction
):

    reference = (
        f"TEST-{uuid.uuid4().hex[:12].upper()}"
    )

    payload = {
        "recipientEmail": recipient["email"],
        "amount": float(amount),
        "description": (
            f"Test transfer from "
            f"{sender['firstName']} "
            f"to {recipient['firstName']}"
        ),
        "referenceNote": reference
    }

    status, response = request_json(
        "POST",
        SEND_MONEY_URL,
        payload,
        token
    )

    if 200 <= status < 300:

        payment_id = None
        payment_status = None

        try:
            payment_id = response["data"]["id"]
            payment_status = response["data"]["status"]
        except (KeyError, TypeError):
            pass

        print(
            f"   ✅ {direction:<8} "
            f"{sender['email']:<38} "
            f"→ {recipient['email']:<38} "
            f"R{amount:,.2f}"
        )

        if payment_id:
            print(
                f"      Payment: {payment_id} "
                f"[{payment_status}]"
            )

        return True

    print(
        f"   ❌ {direction:<8} "
        f"{sender['email']} "
        f"→ {recipient['email']} "
        f"R{amount:,.2f} "
        f"HTTP {status}"
    )

    print(
        f"      {response}"
    )

    return False


# =========================================================
# LOAD USERS
# =========================================================

with open(
    "users.json",
    "r",
    encoding="utf-8"
) as file:

    users = json.load(file)


if len(users) < 2:
    raise RuntimeError(
        "At least 2 users are required."
    )


print()
print("=" * 80)
print("SAFIPAY P2P TRANSFER SEEDER")
print("=" * 80)

print(
    f"Users: {len(users)}"
)

print(
    f"Transfer range: "
    f"R{MIN_AMOUNT:,.2f} - "
    f"R{MAX_AMOUNT:,.2f}"
)

print(
    f"Rounds per pair: {ROUNDS_PER_PAIR}"
)

print()


# =========================================================
# LOGIN ALL USERS ONCE
# =========================================================

print("Logging users in...")
print()

tokens = {}

for user in users:

    token = login(user)

    if token:

        tokens[user["email"]] = token

        print(
            f"✅ {user['email']}"
        )


print()
print(
    f"Logged in: "
    f"{len(tokens)}/{len(users)} users"
)


# =========================================================
# KEEP ONLY SUCCESSFULLY LOGGED-IN USERS
# =========================================================

active_users = [
    user
    for user in users
    if user["email"] in tokens
]


if len(active_users) < 2:

    raise RuntimeError(
        "Not enough successfully logged-in users."
    )


# =========================================================
# RANDOMISE USER ORDER
# =========================================================

random.shuffle(active_users)


# =========================================================
# CREATE PAIRS
# =========================================================

pairs = []

for i in range(
    0,
    len(active_users) - 1,
    2
):

    pairs.append(
        (
            active_users[i],
            active_users[i + 1]
        )
    )


print()
print("=" * 80)

print(
    f"Created {len(pairs)} user pairs"
)

print("=" * 80)


# =========================================================
# STATISTICS
# =========================================================

successful = 0
failed = 0
attempted = 0


# =========================================================
# PROCESS PAIRS
# =========================================================

for pair_number, pair in enumerate(
    pairs,
    start=1
):

    user_a = pair[0]
    user_b = pair[1]

    token_a = tokens[user_a["email"]]
    token_b = tokens[user_b["email"]]

    print()
    print("=" * 80)

    print(
        f"PAIR {pair_number}"
    )

    print(
        f"{user_a['email']}  ↔  "
        f"{user_b['email']}"
    )

    print("=" * 80)


    for round_number in range(
        1,
        ROUNDS_PER_PAIR + 1
    ):

        amount = random_amount()

        print()
        print(
            f"Round {round_number} "
            f"- R{amount:,.2f}"
        )


        # =================================================
        # A → B
        # =================================================

        attempted += 1

        forward_success = send_money(
            sender=user_a,
            recipient=user_b,
            token=token_a,
            amount=amount,
            direction="FORWARD"
        )

        if forward_success:

            successful += 1

        else:

            failed += 1

            print(
                "   ⚠️ Return transfer skipped "
                "because forward transfer failed."
            )

            continue


        # Small pause between transactions
        time.sleep(0.15)


        # =================================================
        # B → A
        #
        # Same amount is returned.
        #
        # This creates realistic payment history while
        # keeping the users' net balances approximately
        # unchanged.
        # =================================================

        attempted += 1

        return_success = send_money(
            sender=user_b,
            recipient=user_a,
            token=token_b,
            amount=amount,
            direction="RETURN"
        )

        if return_success:

            successful += 1

        else:

            failed += 1


        time.sleep(0.15)


# =========================================================
# SUMMARY
# =========================================================

print()
print()
print("=" * 80)
print("P2P TRANSFER SEED COMPLETE")
print("=" * 80)

print(
    f"Pairs:                {len(pairs)}"
)

print(
    f"Rounds per pair:      {ROUNDS_PER_PAIR}"
)

print(
    f"Transactions attempted: {attempted}"
)

print(
    f"Successful:           {successful}"
)

print(
    f"Failed:               {failed}"
)

print("=" * 80)