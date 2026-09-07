from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # Ollama Cloud
    ollama_api_key: str
    ollama_host: str = "https://ollama.com"
    ollama_model: str = "gpt-oss:120b"

    # SafiPay internal services
    wallet_service_url: str = "http://wallet-service:8082"
    payment_service_url: str = "http://payment-service:8083"
    stokvel_service_url: str = "http://stokvel-service:8084"
    merchant_service_url: str = "http://merchant-service:8086"

    request_timeout_seconds: float = 8.0

    model_config = SettingsConfigDict(
        env_file=".env",
        case_sensitive=False,
        extra="ignore",
    )


@lru_cache
def get_settings() -> Settings:
    return Settings()
