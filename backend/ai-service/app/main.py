from typing import Annotated

from fastapi import FastAPI, Header, HTTPException

from .assistant import answer_question
from .config import get_settings
from .safipay_client import build_context
from .schemas import AskRequest, AskResponse


app = FastAPI(
    title="SafiPay AI Service",
    version="1.0.0",
)


@app.get("/actuator/health")
def health():
    return {
        "status": "UP",
        "provider": "ollama-cloud",
        "model": get_settings().ollama_model,
    }


@app.get("/health")
def simple_health():
    return {"status": "UP"}


@app.post("/api/ai/ask", response_model=AskResponse)
def ask(
    request: AskRequest,
    authorization: Annotated[str | None, Header()] = None,
):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing bearer token")

    context, sources = build_context(
        question=request.question,
        authorization=authorization,
    )

    try:
        answer = answer_question(
            question=request.question,
            context=context,
        )
    except Exception as exc:
        # Do not expose provider internals or secrets to the frontend.
        raise HTTPException(
            status_code=502,
            detail="Ollama Cloud request failed",
        ) from exc

    return AskResponse(
        answer=answer,
        sources=sources,
    )
