import json
import random
import re
import urllib.request
import urllib.error
from pathlib import Path


# =========================================================
# CONFIG
# =========================================================

BASE_URL = "http://localhost:8080"

LOGIN_URL = f"{BASE_URL}/api/auth/login"
STOKVEL_URL = f"{BASE_URL}/api/stokvels"

USERS_PER_STOKVEL = 10

MIN_CONTRIBUTION = 250
MAX_CONTRIBUTION = 2500

ACTIVATE_AFTER_CREATION = False

STOKVEL_MODEL_FILE = Path(
    "backend/stokvel-service/src/main/java/"
    "com/safipay/stokvel/model/Stokvel.java"
)


STOKVEL_NAMES = [
    {
        "name": "Ubuntu Savings Circle",
        "description": "A community savings group focused on helping members reach their financial goals."
    },
    {
        "name": "Mzansi Future Fund",
        "description": "A stokvel for members saving together towards future expenses and opportunities."
    },
    {
        "name": "Sisonke Savings Club",
        "description": "A collaborative savings group where members contribute regularly and grow together."
    },
    {
        "name": "Bokamoso Wealth Circle",
        "description": "A savings circle focused on building stronger financial habits and shared prosperity."
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
# READ ENUM VALUES DIRECTLY FROM JAVA
# =========================================================

def extract_enum_values(java_source, enum_name):

    pattern = rf'enum\s+{re.escape(enum_name)}\s*\{{(.*?)\}}'

    match = re.search(
        pattern,
        java_source,
        re.DOTALL
    )

    if not match:
        raise RuntimeError(
            f"Could not find enum {enum_name} in Stokvel.java"
        )

    body = match.group(1)

    # Enum constants normally appear before the first ;
    constants_section = body.split(";")[0]

    values = re.findall(
        r'(?:^|,)\s*([A-Z][A-Z0-9_]*)',
        constants_section,
        re.MULTILINE
    )

    if not values:
        raise RuntimeError(
            f"Could not determine values for enum {enum_name}"
        )

    return values


def load_stokvel_enums():

    if not STOKVEL_MODEL_FILE.exists():
        raise RuntimeError(
            f"Cannot find:\n{STOKVEL_MODEL_FILE}"
        )

    source = STOKVEL_MODEL_FILE.read_text(
        encoding="utf-8"
    )

    stokvel_types = extract_enum_values(
        source,
        "StokvelType"
    )

    frequencies = extract_enum_values(
        source,
        "ContributionFrequency"
    )

    return stokvel_types, frequencies


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
            f"❌ Token missing for "
            f"{user['email']}"
        )

        print(response)

        return None


# =========================================================
# CREATE STOKVEL
# =========================================================

def create_stokvel(
    creator,
    token,
    stokvel_number,
    stokvel_type,
    frequency
):

    info = STOKVEL_NAMES[
        stokvel_number % len(STOKVEL_NAMES)
    ]

    contribution = round(
        random.uniform(
            MIN_CONTRIBUTION,
            MAX_CONTRIBUTION
        ),
        2
    )

    payload = {
        "name": info["name"],
        "description": info["description"],
        "type": stokvel_type,
        "contributionAmount": contribution,
        "contributionFrequency": frequency,
        "maxMembers": USERS_PER_STOKVEL
    }

    status, response = request_json(
        "POST",
        STOKVEL_URL,
        payload,
        token
    )

    if status != 201:

        print()
        print(
            f"❌ FAILED TO CREATE "
            f"{info['name']}"
        )

        print(f"HTTP {status}")
        print(response)

        return None

    try:
        stokvel_id = response["data"]["id"]

    except (KeyError, TypeError):

        print(
            "❌ Stokvel was created but "
            "could not find data.id"
        )

        print(response)

        return None

    print()
    print("=" * 65)

    print(
        f"🏦 CREATED: {info['name']}"
    )

    print(
        f"ID: {stokvel_id}"
    )

    print(
        f"Creator: {creator['email']}"
    )

    print(
        f"Type: {stokvel_type}"
    )

    print(
        f"Frequency: {frequency}"
    )

    print(
        f"Contribution: R{contribution:,.2f}"
    )

    print("=" * 65)

    return stokvel_id


# =========================================================
# JOIN STOKVEL
# =========================================================

def join_stokvel(stokvel_id, user, token):

    status, response = request_json(
        "POST",
        f"{STOKVEL_URL}/{stokvel_id}/join",
        None,
        token
    )

    if 200 <= status < 300:

        print(
            f"   ✅ JOINED: "
            f"{user['email']}"
        )

        return True

    print(
        f"   ❌ JOIN FAILED: "
        f"{user['email']} HTTP {status}"
    )

    print(
        f"      {response}"
    )

    return False


# =========================================================
# GET STOKVEL
# =========================================================

def get_stokvel(stokvel_id, token):

    status, response = request_json(
        "GET",
        f"{STOKVEL_URL}/{stokvel_id}",
        None,
        token
    )

    if status != 200:
        return None

    try:
        return response["data"]
    except (KeyError, TypeError):
        return None


# =========================================================
# ACTIVATE
# =========================================================

def activate_stokvel(
    stokvel_id,
    creator,
    token
):

    status, response = request_json(
        "POST",
        f"{STOKVEL_URL}/{stokvel_id}/activate",
        None,
        token
    )

    if 200 <= status < 300:

        print(
            f"   🚀 ACTIVATED by "
            f"{creator['email']}"
        )

        return True

    print(
        f"   ❌ ACTIVATION FAILED "
        f"HTTP {status}"
    )

    print(response)

    return False


# =========================================================
# MAIN
# =========================================================

with open(
    "users.json",
    "r",
    encoding="utf-8"
) as file:

    users = json.load(file)


if len(users) < USERS_PER_STOKVEL:
    raise RuntimeError(
        f"Need at least "
        f"{USERS_PER_STOKVEL} users."
    )


# ---------------------------------------------------------
# Discover actual Java enum values
# ---------------------------------------------------------

stokvel_types, frequencies = load_stokvel_enums()


print()
print("Discovered StokvelType values:")

for value in stokvel_types:
    print(f"   • {value}")


print()
print("Discovered ContributionFrequency values:")

for value in frequencies:
    print(f"   • {value}")


# ---------------------------------------------------------
# Split users into groups of 10
# ---------------------------------------------------------

groups = [
    users[i:i + USERS_PER_STOKVEL]
    for i in range(
        0,
        len(users),
        USERS_PER_STOKVEL
    )
]


# Ignore incomplete group
groups = [
    group
    for group in groups
    if len(group) == USERS_PER_STOKVEL
]


print()
print(
    f"Creating {len(groups)} stokvels "
    f"with {USERS_PER_STOKVEL} users each..."
)


created_stokvels = []


# =========================================================
# CREATE EACH STOKVEL
# =========================================================

for index, group in enumerate(groups):

    creator = group[0]

    creator_token = login(creator)

    if not creator_token:
        continue


    # Cycle through the actual enum values found
    stokvel_type = stokvel_types[
        index % len(stokvel_types)
    ]

    frequency = frequencies[
        index % len(frequencies)
    ]


    stokvel_id = create_stokvel(
        creator,
        creator_token,
        index,
        stokvel_type,
        frequency
    )

    if stokvel_id is None:
        continue


    # -----------------------------------------------------
    # Creator is normally added during creation.
    #
    # The remaining 9 users join using:
    #
    # POST /api/stokvels/{id}/join
    # -----------------------------------------------------

    successful_members = 1

    print()
    print(
        f"   👑 CREATOR: "
        f"{creator['email']}"
    )


    for member in group[1:]:

        token = login(member)

        if not token:
            continue

        if join_stokvel(
            stokvel_id,
            member,
            token
        ):
            successful_members += 1


    # -----------------------------------------------------
    # Verify member count from actual API
    # -----------------------------------------------------

    stokvel_data = get_stokvel(
        stokvel_id,
        creator_token
    )

    actual_members = None

    if stokvel_data:

        members = stokvel_data.get("members")

        if isinstance(members, list):
            actual_members = len(members)


    print()

    if actual_members is not None:

        print(
            f"   👥 API MEMBER COUNT: "
            f"{actual_members}"
        )

    else:

        print(
            f"   👥 Successful joins/creator: "
            f"{successful_members}"
        )


    # -----------------------------------------------------
    # Optional activation
    # -----------------------------------------------------

    if ACTIVATE_AFTER_CREATION:

        activate_stokvel(
            stokvel_id,
            creator,
            creator_token
        )


    created_stokvels.append({
        "id": stokvel_id,
        "name": STOKVEL_NAMES[
            index % len(STOKVEL_NAMES)
        ]["name"],
        "creator": creator["email"],
        "members": actual_members
        if actual_members is not None
        else successful_members,
        "type": stokvel_type,
        "frequency": frequency
    })


# =========================================================
# SUMMARY
# =========================================================

print()
print()
print("=" * 65)
print("STOKVEL SEED COMPLETE")
print("=" * 65)


for stokvel in created_stokvels:

    print(
        f"🏦 {stokvel['name']}"
    )

    print(
        f"   ID: {stokvel['id']}"
    )

    print(
        f"   Creator: {stokvel['creator']}"
    )

    print(
        f"   Members: {stokvel['members']}"
    )

    print(
        f"   Type: {stokvel['type']}"
    )

    print(
        f"   Frequency: {stokvel['frequency']}"
    )

    print()


print(
    f"Created: "
    f"{len(created_stokvels)} stokvels"
)