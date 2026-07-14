from fastapi import FastAPI

from app.routes.chat import router

app = FastAPI(
    title="Study Assistant AI"
)

app.include_router(router)