CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE documents (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    tenant_id UUID,
    filename TEXT NOT NULL,
    title TEXT,
    content_type TEXT,
    md5 CHAR(32) NOT NULL,
    size_bytes BIGINT,
    raw_markdown TEXT,
    extracted_text TEXT,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    status TEXT NOT NULL,
    error_message TEXT,
    CONSTRAINT fk_documents_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_documents_user_md5 UNIQUE (user_id, md5)
);

CREATE INDEX idx_documents_user_id ON documents(user_id);
CREATE INDEX idx_documents_tenant_id ON documents(tenant_id);

CREATE TABLE document_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    user_id UUID NOT NULL,
    tenant_id UUID,
    chunk_index INT NOT NULL,
    heading_path TEXT,
    start_char INT,
    end_char INT,
    chunk_text TEXT,
    token_count_est INT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_chunks_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE INDEX idx_chunks_document_id ON document_chunks(document_id);
CREATE INDEX idx_chunks_user_id ON document_chunks(user_id);

CREATE TABLE chunk_embeddings (
    chunk_id UUID PRIMARY KEY,
    embedding vector(1536),
    model TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_embeddings_chunk FOREIGN KEY (chunk_id) REFERENCES document_chunks(id) ON DELETE CASCADE
);

CREATE INDEX idx_chunk_embeddings_embedding ON chunk_embeddings USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

CREATE TABLE chats (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    tenant_id UUID,
    title TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_chats_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_chats_user_id ON chats(user_id);

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY,
    chat_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role TEXT NOT NULL,
    content TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_messages_chat FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE
);

CREATE INDEX idx_messages_chat_id ON chat_messages(chat_id);

CREATE TABLE message_retrievals (
    message_id UUID NOT NULL,
    chunk_id UUID NOT NULL,
    score FLOAT,
    rank INT,
    CONSTRAINT fk_retrievals_message FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_retrievals_chunk FOREIGN KEY (chunk_id) REFERENCES document_chunks(id) ON DELETE CASCADE,
    PRIMARY KEY (message_id, chunk_id)
);
