def retrieve_chunks(knowledge, question):

    question = question.lower()

    relevant_chunks = []

    for chunk in knowledge["chunks"]:

        keywords = [k.lower() for k in chunk.get("keywords", [])]

        score = sum(
            1
            for keyword in keywords
            if keyword in question
        )

        if score > 0:
            relevant_chunks.append((score, chunk))

    relevant_chunks.sort(
        key=lambda x: x[0],
        reverse=True
    )

    if not relevant_chunks:
        return knowledge["chunks"][:3]

    return [
        chunk
        for _, chunk in relevant_chunks[:5]
    ]