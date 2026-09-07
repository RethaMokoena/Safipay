import json
import urllib.request
import urllib.error


BASE_URL = "http://localhost:8080"

LOGIN_URL = f"{BASE_URL}/api/auth/login"
MERCHANTS_URL = f"{BASE_URL}/api/merchants"


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
            f"❌ Could not get token for "
            f"{user['email']}"
        )

        print(response)

        return None


# =========================================================
# LOAD USERS
# =========================================================

with open(
    "users.json",
    "r",
    encoding="utf-8"
) as file:

    users = json.load(file)


# Create quick email -> user lookup
users_by_email = {
    user["email"].lower(): user
    for user in users
}


# =========================================================
# LOAD MERCHANTS
# =========================================================

with open(
    "created_merchants.json",
    "r",
    encoding="utf-8"
) as file:

    merchants = json.load(file)


print()
print("=" * 70)
print("SAFIPAY MERCHANT APPROVAL")
print("=" * 70)

print(
    f"Merchants found: {len(merchants)}"
)

print()


successful = 0
failed = 0


# =========================================================
# APPROVE EACH MERCHANT
# =========================================================

for merchant in merchants:

    merchant_id = merchant["id"]
    business_name = merchant["businessName"]
    owner_email = merchant["ownerEmail"]


    # -----------------------------------------------------
    # Find merchant owner in users.json
    # -----------------------------------------------------

    owner = users_by_email.get(
        owner_email.lower()
    )

    if owner is None:

        print(
            f"❌ {business_name:<30} "
            f"Owner not found: {owner_email}"
        )

        failed += 1
        continue


    # -----------------------------------------------------
    # Login owner
    # -----------------------------------------------------

    token = login(owner)

    if not token:

        print(
            f"❌ {business_name:<30} "
            f"Owner login failed"
        )

        failed += 1
        continue


    # -----------------------------------------------------
    # Approve merchant
    # -----------------------------------------------------

    approve_url = (
        f"{MERCHANTS_URL}/"
        f"{merchant_id}/approve"
    )

    status, response = request_json(
        "POST",
        approve_url,
        {},
        token
    )


    if 200 <= status < 300:

        successful += 1

        try:

            updated = response["data"]

            merchant_status = updated["status"]
            wallet_id = updated.get("walletId")

        except (KeyError, TypeError):

            merchant_status = "ACTIVE"
            wallet_id = merchant.get("walletId")


        merchant["status"] = merchant_status

        if wallet_id:
            merchant["walletId"] = wallet_id


        print(
            f"✅ {business_name:<30} "
            f"{merchant_status}"
        )

        print(
            f"   Owner:  {owner_email}"
        )

        print(
            f"   Wallet: {merchant.get('walletId')}"
        )

    else:

        failed += 1

        print(
            f"❌ {business_name:<30} "
            f"HTTP {status}"
        )

        print(
            f"   {response}"
        )

    print()


# =========================================================
# SAVE UPDATED MERCHANT DATA
# =========================================================

with open(
    "created_merchants.json",
    "w",
    encoding="utf-8"
) as file:

    json.dump(
        merchants,
        file,
        indent=4
    )


# =========================================================
# SUMMARY
# =========================================================

print()
print("=" * 70)
print("APPROVAL COMPLETE")
print("=" * 70)

print(
    f"Successful: {successful}"
)

print(
    f"Failed:     {failed}"
)

print("=" * 70)