from fastapi import APIRouter

from app.models.chat import ChatRequest
from app.services.gemini_service import ask_gemini
from app.services.knowledge_service import load_knowledge
from app.services.retrieval_service import retrieve_chunks

router = APIRouter()


@router.post("/chat")
def chat(request: ChatRequest):

    knowledge = load_knowledge(request.uploadId)

    top_chunks = retrieve_chunks(
        knowledge,
        request.question
    )

    study_material = "\n\n".join(
        chunk["text"]
        for chunk in top_chunks
        if chunk.get("text")
    )

    prompt = f"""
You are an academic study assistant.

Answer ONLY using the supplied study material.

If the answer is not present, reply exactly:

I could not find that information in this document.

Study Material:

{study_material}

Question:

{request.question}
"""

    answer = ask_gemini(prompt)

    # -----------------------------
    # Build sources and images
    # -----------------------------

    sources = []

    images = []

    for chunk in top_chunks:

        sources.append({
            "chunkId": chunk["chunkId"],
            "title": chunk.get("chunkTitle"),
            "startPage": chunk["startPage"],
            "endPage": chunk["endPage"]
        })

        images.extend(chunk.get("relatedImages", []))

    return {
        "answer": answer,
        "sources": sources,
        "images": images
    }