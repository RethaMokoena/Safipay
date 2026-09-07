#!/usr/bin/env python3
"""
Seed SafiPay marketplace listings for EVERY test user who owns an ACTIVE merchant.

What this script does:
1. Reads test users from users.json.
2. Logs in as each user through SafiPay's /api/auth/login endpoint.
3. Calls /api/merchants/my using that user's JWT.
4. Skips users who do not own a merchant.
5. Skips merchants that are not ACTIVE.
6. Seeds category-appropriate listings.
7. Skips duplicate listing titles.

Usage from the SafiPay repository root:

    python seed_all_marketplace.py

Optional environment variables:

    SAFIPAY_BASE_URL=http://localhost:8080
    USERS_FILE=users.json
    SEED_PASSWORD=<fallback password if users.json does not contain one>

The script does not print JWTs or passwords.
"""

import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path


BASE_URL = os.getenv("SAFIPAY_BASE_URL", "http://localhost:8080").rstrip("/")
USERS_FILE = Path(os.getenv("USERS_FILE", "users.json"))
FALLBACK_PASSWORD = os.getenv("SEED_PASSWORD", "")


SEED_DATA = {
    "RETAIL": [
        {
            "title": "Fresh Produce Box",
            "description": "A mixed box of fresh fruit and vegetables.",
            "price": 199.99,
            "type": "PRODUCT",
            "stockQuantity": 20,
        },
        {
            "title": "Household Essentials Pack",
            "description": "A practical bundle of everyday household essentials.",
            "price": 149.99,
            "type": "PRODUCT",
            "stockQuantity": 35,
        },
        {
            "title": "2L Orange Juice",
            "description": "Two litre bottle of orange juice.",
            "price": 39.99,
            "type": "PRODUCT",
            "stockQuantity": 50,
        },
        {
            "title": "Snack Combo",
            "description": "A selection of popular snacks for home or work.",
            "price": 79.99,
            "type": "PRODUCT",
            "stockQuantity": 40,
        },
    ],

    "FOOD_BEVERAGE": [
        {
            "title": "Beef Burger",
            "description": "Beef burger served with chips.",
            "price": 85.00,
            "type": "PRODUCT",
            "stockQuantity": 30,
        },
        {
            "title": "Chicken Wrap",
            "description": "Grilled chicken wrap with fresh salad.",
            "price": 70.00,
            "type": "PRODUCT",
            "stockQuantity": 25,
        },
        {
            "title": "Family Meal",
            "description": "A family-sized meal suitable for sharing.",
            "price": 249.99,
            "type": "PRODUCT",
            "stockQuantity": 15,
        },
        {
            "title": "Catering Service",
            "description": "Catering for parties and small events.",
            "price": 1500.00,
            "type": "SERVICE",
        },
    ],

    "HEALTH_BEAUTY": [
        {
            "title": "Haircut",
            "description": "Standard professional haircut.",
            "price": 180.00,
            "type": "SERVICE",
        },
        {
            "title": "Manicure",
            "description": "Classic manicure service.",
            "price": 220.00,
            "type": "SERVICE",
        },
        {
            "title": "Skin Care Set",
            "description": "A basic daily skin care set.",
            "price": 299.99,
            "type": "PRODUCT",
            "stockQuantity": 18,
        },
        {
            "title": "Hair Care Pack",
            "description": "Shampoo, conditioner and treatment pack.",
            "price": 249.99,
            "type": "PRODUCT",
            "stockQuantity": 22,
        },
    ],

    "TRANSPORT": [
        {
            "title": "Airport Transfer",
            "description": "Private transport to OR Tambo International Airport.",
            "price": 450.00,
            "type": "SERVICE",
        },
        {
            "title": "Local Trip",
            "description": "Local point-to-point transport service.",
            "price": 120.00,
            "type": "SERVICE",
        },
        {
            "title": "Daily Shuttle",
            "description": "Scheduled daily shuttle service.",
            "price": 80.00,
            "type": "SERVICE",
        },
        {
            "title": "Event Transport",
            "description": "Transport service for events and group outings.",
            "price": 650.00,
            "type": "SERVICE",
        },
    ],

    "EDUCATION": [
        {
            "title": "Math Tutoring Session",
            "description": "One-hour mathematics tutoring session.",
            "price": 180.00,
            "type": "SERVICE",
        },
        {
            "title": "Programming Tutoring Session",
            "description": "One-hour beginner programming tutoring session.",
            "price": 220.00,
            "type": "SERVICE",
        },
        {
            "title": "Study Notes Pack",
            "description": "A digital study notes pack for revision.",
            "price": 99.99,
            "type": "PRODUCT",
            "stockQuantity": 100,
        },
        {
            "title": "Exam Preparation Session",
            "description": "Focused exam preparation and revision session.",
            "price": 250.00,
            "type": "SERVICE",
        },
    ],

    "ENTERTAINMENT": [
        {
            "title": "Event Ticket",
            "description": "General admission event ticket.",
            "price": 150.00,
            "type": "PRODUCT",
            "stockQuantity": 100,
        },
        {
            "title": "DJ Service",
            "description": "DJ service for private events.",
            "price": 1200.00,
            "type": "SERVICE",
        },
        {
            "title": "Photography Session",
            "description": "One-hour event or portrait photography session.",
            "price": 650.00,
            "type": "SERVICE",
        },
        {
            "title": "Party Package",
            "description": "Entertainment package for small celebrations.",
            "price": 950.00,
            "type": "SERVICE",
        },
    ],

    "SERVICES": [
        {
            "title": "Home Cleaning",
            "description": "Standard home cleaning service.",
            "price": 350.00,
            "type": "SERVICE",
        },
        {
            "title": "Computer Setup",
            "description": "Basic computer setup and software configuration.",
            "price": 300.00,
            "type": "SERVICE",
        },
        {
            "title": "Website Consultation",
            "description": "One-hour website and digital presence consultation.",
            "price": 450.00,
            "type": "SERVICE",
        },
        {
            "title": "Delivery Service",
            "description": "Local delivery service.",
            "price": 100.00,
            "type": "SERVICE",
        },
    ],

    "UTILITIES": [
        {
            "title": "Prepaid Electricity Assistance",
            "description": "Assistance with prepaid electricity purchases.",
            "price": 25.00,
            "type": "SERVICE",
        },
        {
            "title": "Water Delivery",
            "description": "Local bottled water delivery service.",
            "price": 120.00,
            "type": "SERVICE",
        },
        {
            "title": "Gas Refill Service",
            "description": "Household gas refill service.",
            "price": 350.00,
            "type": "SERVICE",
        },
        {
            "title": "Utility Payment Assistance",
            "description": "Assistance with supported utility payments.",
            "price": 20.00,
            "type": "SERVICE",
        },
    ],

    "OTHER": [
        {
            "title": "General Product",
            "description": "A sample marketplace product.",
            "price": 99.99,
            "type": "PRODUCT",
            "stockQuantity": 25,
        },
        {
            "title": "General Service",
            "description": "A sample marketplace service.",
            "price": 250.00,
            "type": "SERVICE",
        },
    ],
}


def request_json(method, path, token=None, body=None):
    url = f"{BASE_URL}{path}"

    headers = {
        "Accept": "application/json",
    }

    if token:
        headers["Authorization"] = f"Bearer {token}"

    data = None
    if body is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(body).encode("utf-8")

    req = urllib.request.Request(
        url=url,
        data=data,
        headers=headers,
        method=method,
    )

    try:
        with urllib.request.urlopen(req, timeout=20) as response:
            raw = response.read().decode("utf-8")
            payload = json.loads(raw) if raw else None
            return response.status, payload

    except urllib.error.HTTPError as exc:
        raw = exc.read().decode("utf-8")
        try:
            payload = json.loads(raw)
        except json.JSONDecodeError:
            payload = raw
        return exc.code, payload

    except urllib.error.URLError as exc:
        print(f"\nCould not connect to SafiPay at {BASE_URL}")
        print(f"Reason: {exc.reason}")
        sys.exit(1)


def load_users():
    if not USERS_FILE.exists():
        print(f"Could not find users file: {USERS_FILE}")
        print()
        print("Either put users.json in the current directory or run:")
        print('  export USERS_FILE="path/to/users.json"')
        sys.exit(1)

    try:
        data = json.loads(USERS_FILE.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        print(f"Could not read {USERS_FILE}: {exc}")
        sys.exit(1)

    if isinstance(data, list):
        return data

    if isinstance(data, dict):
        for key in ("users", "data", "accounts"):
            if isinstance(data.get(key), list):
                return data[key]

    print(
        "Unsupported users.json format. Expected a JSON array or an object "
        "containing a users/data/accounts array."
    )
    sys.exit(1)


def deep_find_token(value):
    """
    Search a login response for common JWT field names without printing it.
    """
    token_keys = {
        "accessToken",
        "access_token",
        "token",
        "jwt",
        "jwtToken",
        "jwt_token",
    }

    if isinstance(value, dict):
        for key, item in value.items():
            if key in token_keys and isinstance(item, str) and item.strip():
                return item.strip()

        for item in value.values():
            found = deep_find_token(item)
            if found:
                return found

    elif isinstance(value, list):
        for item in value:
            found = deep_find_token(item)
            if found:
                return found

    return None


def user_email(user):
    for key in ("email", "userEmail", "username"):
        value = user.get(key)
        if isinstance(value, str) and value.strip():
            return value.strip()
    return None


def user_password(user):
    for key in ("password", "plainPassword", "testPassword"):
        value = user.get(key)
        if isinstance(value, str) and value:
            return value

    return FALLBACK_PASSWORD or None


def login(user):
    email = user_email(user)
    password = user_password(user)

    if not email:
        return None, "missing email/username"

    if not password:
        return None, "missing password"

    # First try the common SafiPay email/password login shape.
    attempts = [
        {"email": email, "password": password},
        {"username": email, "password": password},
    ]

    last_status = None

    for body in attempts:
        status, payload = request_json(
            "POST",
            "/api/auth/login",
            body=body,
        )

        last_status = status

        if 200 <= status < 300:
            token = deep_find_token(payload)
            if token:
                return token, None

    return None, f"login failed (last HTTP status {last_status})"


def get_my_merchants(token):
    status, payload = request_json(
        "GET",
        "/api/merchants/my",
        token=token,
    )

    if status != 200:
        return None, f"merchant lookup failed (HTTP {status})"

    if not isinstance(payload, dict):
        return [], None

    data = payload.get("data", [])
    return data if isinstance(data, list) else [], None


def get_existing_titles(token, merchant_id):
    status, payload = request_json(
        "GET",
        f"/api/merchants/{merchant_id}/listings/manage",
        token=token,
    )

    if status != 200:
        return set(), f"listing lookup failed (HTTP {status})"

    listings = payload.get("data", []) if isinstance(payload, dict) else []

    return {
        str(item.get("title", "")).strip().lower()
        for item in listings
        if isinstance(item, dict)
    }, None


def create_listing(token, merchant_id, listing):
    return request_json(
        "POST",
        f"/api/merchants/{merchant_id}/listings",
        token=token,
        body=listing,
    )


def main():
    users = load_users()

    print(f"SafiPay API: {BASE_URL}")
    print(f"Users file: {USERS_FILE}")
    print(f"Users loaded: {len(users)}")
    print()

    users_logged_in = 0
    users_with_merchants = 0
    active_merchants = 0
    inactive_merchants = 0
    created = 0
    skipped = 0
    failed = 0

    for index, user in enumerate(users, start=1):
        if not isinstance(user, dict):
            print(f"[{index}/{len(users)}] SKIP malformed user record")
            failed += 1
            continue

        email = user_email(user) or f"user #{index}"

        print(f"[{index}/{len(users)}] Login: {email}")

        token, error = login(user)

        if not token:
            print(f"  FAILED: {error}")
            failed += 1
            continue

        users_logged_in += 1

        merchants, error = get_my_merchants(token)

        if merchants is None:
            print(f"  FAILED: {error}")
            failed += 1
            continue

        if not merchants:
            print("  No merchant account")
            continue

        users_with_merchants += 1

        for merchant in merchants:
            merchant_id = merchant.get("id")
            merchant_name = merchant.get("businessName", merchant_id)
            category = merchant.get("category", "OTHER")
            status = merchant.get("status")

            print(
                f"  Merchant: {merchant_name} "
                f"[{category}] [{status}]"
            )

            if status != "ACTIVE":
                print("    SKIP merchant is not ACTIVE")
                inactive_merchants += 1
                continue

            active_merchants += 1

            existing_titles, error = get_existing_titles(
                token,
                merchant_id,
            )

            if error:
                print(f"    FAILED: {error}")
                failed += 1
                continue

            seed_items = SEED_DATA.get(
                category,
                SEED_DATA["OTHER"],
            )

            for listing in seed_items:
                title = listing["title"]

                if title.lower() in existing_titles:
                    print(f"    SKIP    {title} (already exists)")
                    skipped += 1
                    continue

                http_status, payload = create_listing(
                    token,
                    merchant_id,
                    listing,
                )

                if http_status == 201:
                    print(f"    CREATED {title}")
                    created += 1
                    existing_titles.add(title.lower())
                else:
                    message = payload
                    if isinstance(payload, dict):
                        message = (
                            payload.get("message")
                            or payload.get("error")
                            or payload
                        )

                    print(
                        f"    FAILED  {title} "
                        f"(HTTP {http_status}): {message}"
                    )
                    failed += 1

        print()

    print("=" * 60)
    print("SafiPay marketplace seed summary")
    print("=" * 60)
    print(f"Users loaded:          {len(users)}")
    print(f"Users logged in:       {users_logged_in}")
    print(f"Users with merchants:  {users_with_merchants}")
    print(f"ACTIVE merchants:      {active_merchants}")
    print(f"Inactive merchants:    {inactive_merchants}")
    print(f"Listings created:      {created}")
    print(f"Listings skipped:      {skipped}")
    print(f"Failures:              {failed}")
    print("=" * 60)


if __name__ == "__main__":
    main()
