from pydantic import BaseModel


class ChatRequest(BaseModel):
    uploadId: str
    question: str