import hashlib
import re
from decimal import Decimal, InvalidOperation
from typing import Any

import httpx

from .config import get_settings


SENSITIVE_KEYS = {
    "password",
    "accessToken",
    "refreshToken",
    "token",
    "jwt",
    "email",
    "userEmail",
    "businessEmail",
    "businessPhone",
    "phone",
    "phoneNumber",
    "nationalId",
}

IDENTIFIER_KEYS = {
    "id",
    "userId",
    "ownerUserId",
    "senderUserId",
    "recipientUserId",
    "payerUserId",
    "walletId",
    "merchantId",
    "paymentId",
}


# =========================================================
# SANITISATION
# =========================================================

def _alias(value: Any) -> str:
    digest = hashlib.sha256(
        str(value).encode("utf-8")
    ).hexdigest()[:10]

    return f"id_{digest}"


def sanitize(
    value: Any,
    key: str | None = None
) -> Any:

    """
    Remove personal/auth data before sending SafiPay context
    to Ollama Cloud.

    IDs become stable aliases so the AI can still distinguish
    records without receiving the real database IDs.
    """

    if key in SENSITIVE_KEYS:
        return None

    if key in IDENTIFIER_KEYS and value is not None:
        return _alias(value)

    if isinstance(value, dict):

        clean: dict[str, Any] = {}

        for k, v in value.items():

            if k in SENSITIVE_KEYS:
                continue

            sanitized = sanitize(v, k)

            if sanitized is not None:
                clean[k] = sanitized

        return clean

    if isinstance(value, list):
        return [
            sanitize(item)
            for item in value
        ]

    return value


# =========================================================
# HELPERS
# =========================================================

def _to_decimal(value: Any) -> Decimal | None:

    if value is None:
        return None

    try:
        return Decimal(str(value))
    except (InvalidOperation, ValueError, TypeError):
        return None


def _normalise_items(value: Any) -> list[dict[str, Any]]:

    """
    Supports normal lists as well as possible paginated /
    wrapped response formats.
    """

    if isinstance(value, list):

        return [
            item
            for item in value
            if isinstance(item, dict)
        ]

    if isinstance(value, dict):

        for key in (
            "content",
            "items",
            "merchants",
            "stokvels",
            "listings",
        ):

            items = value.get(key)

            if isinstance(items, list):

                return [
                    item
                    for item in items
                    if isinstance(item, dict)
                ]

        return [value]

    return []


# =========================================================
# MERCHANT DISCOVERY
# =========================================================

def _merchant_public_view(
    merchant: dict[str, Any]
) -> dict[str, Any]:

    """
    Only expose fields needed for merchant discovery.
    """

    return {
        "id": merchant.get("id"),
        "businessName": merchant.get("businessName"),
        "category": merchant.get("category"),
        "description": merchant.get("description"),
        "logoUrl": merchant.get("logoUrl"),
        "status": merchant.get("status"),
    }


def build_merchant_catalog(value: Any) -> list[dict[str, Any]]:

    merchants = _normalise_items(value)

    result: list[dict[str, Any]] = []

    for merchant in merchants:

        # /discover should already only return ACTIVE merchants,
        # but this is an extra defensive check.
        status = merchant.get("status")

        if status is not None and status != "ACTIVE":
            continue

        result.append(
            _merchant_public_view(merchant)
        )

    return result


MERCHANT_CATEGORY_HINTS = {

    "FOOD_BEVERAGE": (
        "food",
        "beverage",
        "restaurant",
        "restaurants",
        "drink",
        "drinks",
        "cafe",
        "coffee",
        "eat",
        "eating",
    ),

    "HEALTH_BEAUTY": (
        "health",
        "beauty",
        "salon",
        "spa",
        "hair",
        "cosmetics",
    ),

    "TRANSPORT": (
        "transport",
        "taxi",
        "ride",
        "travel",
    ),

    "EDUCATION": (
        "education",
        "school",
        "learning",
        "training",
        "tutor",
        "course",
    ),

    "ENTERTAINMENT": (
        "entertainment",
        "cinema",
        "movie",
        "music",
        "games",
        "events",
    ),

    "RETAIL": (
        "retail",
        "shop",
        "store",
        "shopping",
    ),

    "SERVICES": (
        "services",
        "service provider",
    ),

    "UTILITIES": (
        "utilities",
        "utility",
    ),
}


def extract_merchant_category(
    question: str
) -> str | None:

    q = question.lower()

    # First support exact enum values such as:
    # FOOD_BEVERAGE
    upper_question = question.upper()

    for category in MERCHANT_CATEGORY_HINTS:

        if category in upper_question:
            return category

    # Then natural language
    for category, keywords in MERCHANT_CATEGORY_HINTS.items():

        if any(keyword in q for keyword in keywords):
            return category

    return None


# =========================================================
# MARKETPLACE LISTING DISCOVERY
# =========================================================

def _marketplace_public_view(
    listing: dict[str, Any]
) -> dict[str, Any]:
    """
    Only expose fields Safi needs to recommend real marketplace
    products/services.
    """

    listing_type = str(
        listing.get("type", "")
    ).upper()

    stock_quantity = listing.get("stockQuantity")

    if listing_type == "PRODUCT":
        available = (
            isinstance(stock_quantity, int)
            and stock_quantity > 0
        )
    else:
        available = True

    return {
        "id": listing.get("id"),
        "merchantId": listing.get("merchantId"),
        "merchantName": listing.get("merchantName"),
        "title": listing.get("title"),
        "description": listing.get("description"),
        "price": listing.get("price"),
        "type": listing.get("type"),
        "stockQuantity": stock_quantity,
        "available": available,
    }


def build_marketplace_catalog(
    value: Any
) -> list[dict[str, Any]]:

    listings = _normalise_items(value)

    result: list[dict[str, Any]] = []

    for listing in listings:

        if not bool(
            listing.get("active", True)
        ):
            continue

        result.append(
            _marketplace_public_view(listing)
        )

    return result


def extract_max_price(
    question: str
) -> Decimal | None:
    """
    Extract common budget expressions such as:
    under R100, below 200, up to R500, budget of R250,
    or R100 or less.
    """

    q = question.lower()

    patterns = (
        r"(?:under|below|less than|up to|at most|maximum(?: of)?|max(?: of)?|budget(?: is| of)?)"
        r"\s*(?:r|zar)?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)",

        r"(?:r|zar)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)"
        r"\s*(?:or less|or below|maximum|max)",
    )

    for pattern in patterns:

        match = re.search(
            pattern,
            q,
            flags=re.IGNORECASE,
        )

        if not match:
            continue

        try:
            return Decimal(
                match.group(1).replace(",", "")
            )
        except InvalidOperation:
            return None

    return None


def extract_listing_type(
    question: str
) -> str | None:

    q = question.lower()

    if any(
        phrase in q
        for phrase in (
            "service",
            "services",
            "appointment",
            "session",
            "consultation",
        )
    ):
        return "SERVICE"

    if any(
        phrase in q
        for phrase in (
            "product",
            "products",
            "item",
            "items",
            "goods",
        )
    ):
        return "PRODUCT"

    return None


# =========================================================
# STOKVEL DISCOVERY
# =========================================================

def _stokvel_public_view(
    stokvel: dict[str, Any]
) -> dict[str, Any]:

    amount = _to_decimal(
        stokvel.get("contributionAmount")
    )

    frequency = str(
        stokvel.get(
            "contributionFrequency",
            ""
        )
    ).upper()

    members = stokvel.get("members")

    member_count: int | None = None

    if isinstance(members, list):
        member_count = len(members)

    elif isinstance(
        stokvel.get("memberCount"),
        int
    ):
        member_count = stokvel["memberCount"]

    elif isinstance(
        stokvel.get("currentMembers"),
        int
    ):
        member_count = stokvel["currentMembers"]

    max_members = stokvel.get("maxMembers")

    available_slots: int | None = None

    if (
        isinstance(max_members, int)
        and member_count is not None
    ):

        available_slots = max(
            max_members - member_count,
            0
        )

    result: dict[str, Any] = {

        "id": stokvel.get("id"),
        "name": stokvel.get("name"),
        "description": stokvel.get("description"),
        "type": stokvel.get("type"),
        "status": stokvel.get("status"),

        "contributionAmount":
            stokvel.get("contributionAmount"),

        "contributionFrequency":
            stokvel.get("contributionFrequency"),

        "maxMembers": max_members,
        "memberCount": member_count,
        "availableSlots": available_slots,
    }

    # Convert different contribution frequencies into
    # comparable monthly/weekly amounts.

    if amount is not None:

        monthly: Decimal | None = None
        weekly: Decimal | None = None

        if frequency == "WEEKLY":

            monthly = (
                amount
                * Decimal("52")
                / Decimal("12")
            )

            weekly = amount

        elif frequency == "BIWEEKLY":

            monthly = (
                amount
                * Decimal("26")
                / Decimal("12")
            )

            weekly = (
                amount
                / Decimal("2")
            )

        elif frequency == "MONTHLY":

            monthly = amount

            weekly = (
                amount
                * Decimal("12")
                / Decimal("52")
            )

        if monthly is not None:

            result[
                "monthlyEquivalentContribution"
            ] = float(
                monthly.quantize(
                    Decimal("0.01")
                )
            )

        if weekly is not None:

            result[
                "weeklyEquivalentContribution"
            ] = float(
                weekly.quantize(
                    Decimal("0.01")
                )
            )

    status = stokvel.get("status")

    result["joinable"] = (
        status in {"FORMING", "ACTIVE"}
        and (
            available_slots is None
            or available_slots > 0
        )
    )

    return result


def build_stokvel_catalog(
    value: Any
) -> list[dict[str, Any]]:

    stokvels = _normalise_items(value)

    return [
        _stokvel_public_view(stokvel)
        for stokvel in stokvels
    ]


# =========================================================
# SAFIPAY HTTP CLIENT
# =========================================================

class SafiPayClient:

    def __init__(self) -> None:
        self.settings = get_settings()

    def _get(
        self,
        base_url: str,
        path: str,
        authorization: str,
        params: dict[str, str] | None = None,
    ) -> Any:

        headers = {
            "Authorization": authorization,
            "Accept": "application/json",
        }

        try:

            with httpx.Client(
                timeout=
                self.settings.request_timeout_seconds
            ) as client:

                response = client.get(
                    f"{base_url}{path}",
                    headers=headers,
                    params=params,
                )

            if response.status_code >= 400:

                return {
                    "available": False,
                    "status":
                        response.status_code,
                    "message":
                        "SafiPay service returned an error.",
                }

            try:

                body = response.json()

            except ValueError:

                return {
                    "available": False,
                    "status":
                        response.status_code,
                    "message":
                        "SafiPay service returned a non-JSON response.",
                }

            # SafiPay commonly wraps responses:
            #
            # {
            #   "data": ...
            # }

            data = (
                body.get("data", body)
                if isinstance(body, dict)
                else body
            )

            return sanitize(data)

        except httpx.RequestError:

            return {
                "available": False,
                "message":
                    "SafiPay service is currently unavailable.",
            }


    # =====================================================
    # PERSONAL WALLET DATA
    # =====================================================

    def wallet(
        self,
        authorization: str
    ) -> Any:

        return self._get(
            self.settings.wallet_service_url,
            "/api/wallets/me",
            authorization,
        )


    def transactions(
        self,
        authorization: str
    ) -> Any:

        return self._get(
            self.settings.wallet_service_url,
            "/api/wallets/transactions",
            authorization,
        )


    def payments(
        self,
        authorization: str
    ) -> Any:

        return self._get(
            self.settings.payment_service_url,
            "/api/payments/history",
            authorization,
        )


    # =====================================================
    # PERSONAL STOKVEL DATA
    # =====================================================

    def my_stokvels(
        self,
        authorization: str
    ) -> Any:

        return self._get(
            self.settings.stokvel_service_url,
            "/api/stokvels/my",
            authorization,
        )


    # =====================================================
    # GENERAL STOKVEL DISCOVERY
    # =====================================================

    def stokvel_catalog(
        self,
        authorization: str
    ) -> Any:

        data = self._get(
            self.settings.stokvel_service_url,
            "/api/stokvels",
            authorization,
        )

        if (
            isinstance(data, dict)
            and data.get("available") is False
        ):
            return data

        return build_stokvel_catalog(data)


    # =====================================================
    # PERSONAL MERCHANT DATA
    # =====================================================

    def my_merchants(
        self,
        authorization: str
    ) -> Any:

        return self._get(
            self.settings.merchant_service_url,
            "/api/merchants/my",
            authorization,
        )


    # =====================================================
    # GENERAL MERCHANT DISCOVERY
    # =====================================================

    def merchant_catalog(
        self,
        authorization: str,
        question: str,
    ) -> Any:

        category = extract_merchant_category(
            question
        )

        params: dict[str, str] = {}

        if category:
            params["category"] = category

        data = self._get(
            self.settings.merchant_service_url,
            "/api/merchants/discover",
            authorization,
            params=params or None,
        )

        if (
            isinstance(data, dict)
            and data.get("available") is False
        ):
            return data

        return build_merchant_catalog(data)


    # =====================================================
    # MARKETPLACE LISTING DISCOVERY
    # =====================================================

    def marketplace_catalog(
        self,
        authorization: str,
        question: str,
    ) -> Any:
        """
        Fetch actual products/services currently listed in
        SafiPay's marketplace.

        Deterministic category/type/budget filters are applied
        before the data is supplied to the language model.
        """

        params: dict[str, str] = {}

        category = extract_merchant_category(
            question
        )

        listing_type = extract_listing_type(
            question
        )

        max_price = extract_max_price(
            question
        )

        if category:
            params["category"] = category

        if listing_type:
            params["type"] = listing_type

        if max_price is not None:
            params["maxPrice"] = format(
                max_price,
                "f"
            )

        data = self._get(
            self.settings.merchant_service_url,
            "/api/merchants/listings",
            authorization,
            params=params or None,
        )

        if (
            isinstance(data, dict)
            and data.get("available") is False
        ):
            return data

        return build_marketplace_catalog(data)


# =========================================================
# QUESTION ROUTING
# =========================================================

def choose_sources(
    question: str
) -> list[str]:

    """
    Determine which SafiPay data sources are needed.

    Personal questions use personal endpoints.

    Discovery questions use SafiPay-wide merchant/stokvel
    catalog endpoints.
    """

    q = question.lower()

    selected: list[str] = []


    # =====================================================
    # WALLET
    # =====================================================

    if any(
        phrase in q
        for phrase in (
            "balance",
            "wallet",
            "available funds",
            "money do i have",
            "funds",
            "cash",
            "can i afford",
            "afford",
        )
    ):

        selected.append("wallet")


    # =====================================================
    # TRANSACTIONS
    # =====================================================

    if any(
        phrase in q
        for phrase in (
            "transaction",
            "transactions",
            "spent",
            "spend",
            "spending",
            "received",
            "recent activity",
            "top up",
            "top-up",
        )
    ):

        selected.append(
            "transactions"
        )


    # =====================================================
    # PAYMENTS
    # =====================================================

    if any(
        phrase in q
        for phrase in (
            "payment",
            "payments",
            "sent",
            "transfer",
            "recipient",
            "paid",
        )
    ):

        selected.append("payments")


    # =====================================================
    # STOKVELS
    # =====================================================

    stokvel_question = any(
        phrase in q
        for phrase in (
            "stokvel",
            "stokvels",
            "contribution",
            "contributions",
            "payout",
            "group savings",
            "savings group",
        )
    )

    if stokvel_question:

        personal_stokvel_question = any(
            phrase in q
            for phrase in (
                "my stokvel",
                "my stokvels",
                "stokvel am i",
                "stokvels am i",
                "am i part of",
                "i belong to",
                "my contribution",
                "my payout",
            )
        )

        if personal_stokvel_question:

            selected.append(
                "my_stokvels"
            )

        else:

            selected.append(
                "stokvel_catalog"
            )


    # =====================================================
    # MARKETPLACE PRODUCTS / SERVICES
    # =====================================================

    marketplace_question = any(
        phrase in q
        for phrase in (
            "buy",
            "buying",
            "purchase",
            "purchasing",
            "where can i get",
            "what can i get",
            "what can i buy",
            "product",
            "products",
            "item",
            "items",
            "goods",
            "price",
            "prices",
            "cost",
            "costs",
            "under r",
            "below r",
            "less than r",
            "food",
            "beverage",
            "burger",
            "wrap",
            "produce",
            "juice",
            "beauty",
            "haircut",
            "manicure",
            "transport",
            "shuttle",
            "transfer",
            "tutoring",
            "ticket",
            "catering",
            "delivery",
            "cleaning",
            "service",
            "services",
        )
    )

    if marketplace_question:

        selected.append(
            "marketplace_catalog"
        )


    # =====================================================
    # MERCHANTS / BUSINESSES
    # =====================================================

    merchant_question = any(
        phrase in q
        for phrase in (
            "merchant",
            "merchants",
            "business",
            "businesses",
            "shop",
            "store",
            "sell",
            "sells",
            "selling",
            "where can i buy",
            "where can i get",
            "food",
            "beverage",
            "restaurant",
            "transport",
            "beauty",
            "salon",
            "education",
            "entertainment",
            "retail",
            "food_beverage",
            "health_beauty",
        )
    )

    if merchant_question:

        personal_merchant_question = any(
            phrase in q
            for phrase in (
                "my business",
                "my businesses",
                "my merchant",
                "my merchants",
                "businesses do i own",
                "business do i own",
                "merchants do i own",
            )
        )

        if personal_merchant_question:

            selected.append(
                "my_merchants"
            )

        else:

            merchant_directory_question = any(
                phrase in q
                for phrase in (
                    "merchant",
                    "merchants",
                    "business",
                    "businesses",
                    "shop",
                    "store",
                    "retail",
                )
            )

            if (
                merchant_directory_question
                or not marketplace_question
            ):

                selected.append(
                    "merchant_catalog"
                )


    # =====================================================
    # GENERAL ACCOUNT OVERVIEW
    # =====================================================

    if not selected:

        selected = [
            "wallet",
            "my_stokvels",
            "my_merchants",
        ]


    # Remove duplicates while preserving order.

    return list(
        dict.fromkeys(selected)
    )


# =========================================================
# BUILD AI CONTEXT
# =========================================================

def build_context(
    question: str,
    authorization: str
) -> tuple[
    dict[str, Any],
    list[str]
]:

    client = SafiPayClient()

    selected = choose_sources(
        question
    )

    context: dict[str, Any] = {}

    for source in selected:

        if source == "wallet":

            context[source] = (
                client.wallet(
                    authorization
                )
            )

        elif source == "transactions":

            context[source] = (
                client.transactions(
                    authorization
                )
            )

        elif source == "payments":

            context[source] = (
                client.payments(
                    authorization
                )
            )

        elif source == "my_stokvels":

            context[source] = (
                client.my_stokvels(
                    authorization
                )
            )

        elif source == "stokvel_catalog":

            context[source] = (
                client.stokvel_catalog(
                    authorization
                )
            )

        elif source == "my_merchants":

            context[source] = (
                client.my_merchants(
                    authorization
                )
            )

        elif source == "merchant_catalog":

            context[source] = (
                client.merchant_catalog(
                    authorization,
                    question,
                )
            )

        elif source == "marketplace_catalog":

            context[source] = (
                client.marketplace_catalog(
                    authorization,
                    question,
                )
            )

    return context, selected