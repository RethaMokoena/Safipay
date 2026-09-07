import json
import random
import urllib.request
import urllib.error

BASE_URL = "http://localhost:8080"

LOGIN_URL = f"{BASE_URL}/api/auth/login"
TOPUP_URL = f"{BASE_URL}/api/wallets/top-up"

MIN_AMOUNT = 1000
MAX_AMOUNT = 50000


def post_json(url, data, token=None):
    headers = {
        "Content-Type": "application/json"
    }

    if token:
        headers["Authorization"] = f"Bearer {token}"

    request = urllib.request.Request(
        url,
        data=json.dumps(data).encode("utf-8"),
        headers=headers,
        method="POST"
    )

    try:
        with urllib.request.urlopen(request) as response:
            body = response.read().decode("utf-8")

            if body:
                return response.status, json.loads(body)

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


with open("users.json", "r", encoding="utf-8") as file:
    users = json.load(file)


print(f"Loading wallets for {len(users)} users...\n")


successful = 0
failed = 0


for user in users:
    email = user["email"]
    password = user["password"]

    # -------------------------
    # 1. LOGIN
    # -------------------------

    login_status, login_response = post_json(
        LOGIN_URL,
        {
            "email": email,
            "password": password
        }
    )

    if login_status != 200:
        print(f"❌ LOGIN FAILED: {email}")
        print(login_response)
        print()

        failed += 1
        continue

    try:
        token = login_response["data"]["accessToken"]

    except (KeyError, TypeError):
        print(f"❌ Could not get access token for {email}")
        print(login_response)
        print()

        failed += 1
        continue


    # -------------------------
    # 2. RANDOM AMOUNT
    # -------------------------

    amount = round(
        random.uniform(MIN_AMOUNT, MAX_AMOUNT),
        2
    )


    # -------------------------
    # 3. TOP UP WALLET
    # -------------------------

    topup_status, topup_response = post_json(
        TOPUP_URL,
        {
            "amount": amount,
            "referenceId": f"SEED-{email}"
        },
        token
    )


    if 200 <= topup_status < 300:
        print(
            f"✅ {email:<40} "
            f"R{amount:,.2f}"
        )

        successful += 1

    else:
        print(
            f"❌ TOPUP FAILED: {email} "
            f"HTTP {topup_status}"
        )

        print(topup_response)

        failed += 1

    print()


print("----------------------------------------")
print("Finished.")
print(f"Successful: {successful}")
print(f"Failed:     {failed}")
print("----------------------------------------")