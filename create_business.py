import json
import urllib.request
import urllib.error


# =========================================================
# CONFIG
# =========================================================

BASE_URL = "http://localhost:8080"

LOGIN_URL = f"{BASE_URL}/api/auth/login"
MERCHANT_URL = f"{BASE_URL}/api/merchants"

NUMBER_OF_MERCHANTS = 9


# =========================================================
# TEST BUSINESSES
# =========================================================

BUSINESSES = [
    {
        "businessName": "Mzansi Fresh Market",
        "category": "RETAIL",
        "businessRegistrationNumber": "2026/000001/07",
        "businessEmail": "accounts@mzansifresh.test",
        "businessPhone": "+27110000001",
        "description": "A neighbourhood retailer selling groceries, household products and fresh produce."
    },
    {
        "businessName": "Ubuntu Kitchen",
        "category": "FOOD_BEVERAGE",
        "businessRegistrationNumber": "2026/000002/07",
        "businessEmail": "payments@ubuntukitchen.test",
        "businessPhone": "+27110000002",
        "description": "A local food business serving South African meals and takeaway orders."
    },
    {
        "businessName": "Glow Beauty Studio",
        "category": "HEALTH_BEAUTY",
        "businessRegistrationNumber": "2026/000003/07",
        "businessEmail": "hello@glowbeauty.test",
        "businessPhone": "+27110000003",
        "description": "Beauty and personal care services for local customers."
    },
    {
        "businessName": "Sisonke Transport",
        "category": "TRANSPORT",
        "businessRegistrationNumber": "2026/000004/07",
        "businessEmail": "billing@sisonketransport.test",
        "businessPhone": "+27110000004",
        "description": "Local passenger and small business transport services."
    },
    {
        "businessName": "Bokamoso Learning Centre",
        "category": "EDUCATION",
        "businessRegistrationNumber": "2026/000005/07",
        "businessEmail": "fees@bokamosolearning.test",
        "businessPhone": "+27110000005",
        "description": "Tutoring and educational support for learners and students."
    },
    {
        "businessName": "Jozi Entertainment Hub",
        "category": "ENTERTAINMENT",
        "businessRegistrationNumber": "2026/000006/07",
        "businessEmail": "bookings@jozihub.test",
        "businessPhone": "+27110000006",
        "description": "Entertainment, events and recreational services."
    },
    {
        "businessName": "Khula Tech Services",
        "category": "SERVICES",
        "businessRegistrationNumber": "2026/000007/07",
        "businessEmail": "payments@khulatech.test",
        "businessPhone": "+27110000007",
        "description": "IT support, software services and technology consulting for small businesses."
    },
    {
        "businessName": "Lethabo Utilities",
        "category": "UTILITIES",
        "businessRegistrationNumber": "2026/000008/07",
        "businessEmail": "accounts@lethaboutilities.test",
        "businessPhone": "+27110000008",
        "description": "Utility related services and prepaid service solutions."
    },
    {
        "businessName": "Thrive Local Trading",
        "category": "OTHER",
        "businessRegistrationNumber": "2026/000009/07",
        "businessEmail": "finance@thrivelocal.test",
        "businessPhone": "+27110000009",
        "description": "A general trading business serving local customers and small enterprises."
    }
]


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
            f"❌ Could not get access token "
            f"for {user['email']}"
        )

        print(response)

        return None


# =========================================================
# CREATE MERCHANT
# =========================================================

def create_merchant(owner, business, token):

    status, response = request_json(
        "POST",
        MERCHANT_URL,
        business,
        token
    )

    if status != 201:

        print()
        print(
            f"❌ MERCHANT CREATION FAILED"
        )

        print(
            f"   Business: {business['businessName']}"
        )

        print(
            f"   Owner:    {owner['email']}"
        )

        print(
            f"   HTTP:     {status}"
        )

        print(
            f"   Response: {response}"
        )

        return None

    try:

        merchant = response["data"]

        merchant_id = merchant["id"]

    except (KeyError, TypeError):

        print(
            f"❌ Merchant created but "
            f"response did not contain data.id"
        )

        print(response)

        return None

    print()
    print("=" * 70)

    print(
        f"🏪 CREATED: {business['businessName']}"
    )

    print(
        f"   Merchant ID: {merchant_id}"
    )

    print(
        f"   Owner:       {owner['email']}"
    )

    print(
        f"   Category:    {business['category']}"
    )

    print(
        f"   Registration: "
        f"{business['businessRegistrationNumber']}"
    )

    print(
        f"   Email:       {business['businessEmail']}"
    )

    print(
        f"   Status:      "
        f"{merchant.get('status', 'UNKNOWN')}"
    )

    print(
        f"   Wallet ID:   "
        f"{merchant.get('walletId')}"
    )

    print("=" * 70)

    return merchant


# =========================================================
# LOAD USERS
# =========================================================

with open(
    "users.json",
    "r",
    encoding="utf-8"
) as file:

    users = json.load(file)


if len(users) < NUMBER_OF_MERCHANTS:

    raise RuntimeError(
        f"Need at least {NUMBER_OF_MERCHANTS} users "
        f"in users.json."
    )


if len(BUSINESSES) < NUMBER_OF_MERCHANTS:

    raise RuntimeError(
        "Not enough business definitions."
    )


# =========================================================
# CREATE 9 MERCHANTS
# =========================================================

print()
print("=" * 70)
print("SAFIPAY MERCHANT SEEDER")
print("=" * 70)

print(
    f"Creating {NUMBER_OF_MERCHANTS} merchants..."
)

created_merchants = []
failed = 0


for index in range(NUMBER_OF_MERCHANTS):

    owner = users[index]

    business = BUSINESSES[index]

    print()
    print(
        f"[{index + 1}/{NUMBER_OF_MERCHANTS}] "
        f"{business['businessName']}"
    )

    print(
        f"Owner: {owner['email']}"
    )


    # -----------------------------------------------------
    # LOGIN OWNER
    # -----------------------------------------------------

    token = login(owner)

    if not token:

        failed += 1
        continue


    # -----------------------------------------------------
    # REGISTER MERCHANT
    # -----------------------------------------------------

    merchant = create_merchant(
        owner,
        business,
        token
    )

    if merchant:

        created_merchants.append({
            "id": merchant.get("id"),
            "businessName": merchant.get(
                "businessName"
            ),
            "ownerEmail": owner["email"],
            "ownerUserId": merchant.get(
                "ownerUserId"
            ),
            "category": merchant.get(
                "category"
            ),
            "status": merchant.get(
                "status"
            ),
            "walletId": merchant.get(
                "walletId"
            )
        })

    else:

        failed += 1


# =========================================================
# SAVE RESULT
# =========================================================

with open(
    "created_merchants.json",
    "w",
    encoding="utf-8"
) as file:

    json.dump(
        created_merchants,
        file,
        indent=4
    )


# =========================================================
# SUMMARY
# =========================================================

print()
print()
print("=" * 70)
print("MERCHANT SEED COMPLETE")
print("=" * 70)

print(
    f"Requested:   {NUMBER_OF_MERCHANTS}"
)

print(
    f"Created:     {len(created_merchants)}"
)

print(
    f"Failed:      {failed}"
)

print()


for merchant in created_merchants:

    print(
        f"🏪 {merchant['businessName']}"
    )

    print(
        f"   ID:     {merchant['id']}"
    )

    print(
        f"   Owner:  {merchant['ownerEmail']}"
    )

    print(
        f"   Status: {merchant['status']}"
    )

    print(
        f"   Wallet: {merchant['walletId']}"
    )

    print()


print(
    "Merchant details saved to "
    "created_merchants.json"
)

print("=" * 70)