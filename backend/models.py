from sqlalchemy import Column, Integer, String, JSON, DateTime
from sqlalchemy.sql import func
from database import Base

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    public_key = Column(String, unique=True, index=True, nullable=False)
    commitment = Column(String, nullable=False)
    id_hash = Column(String, unique=True, index=True, nullable=False)
    encrypted_pii = Column(String, nullable=False) # Stored as JSON string
    enrollment_proof = Column(JSON, nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

class VerificationLog(Base):
    __tablename__ = "verification_logs"

    id = Column(Integer, primary_key=True, index=True)
    nullifier = Column(String, unique=True, index=True, nullable=False)
    proof = Column(JSON, nullable=False)
    verified_at = Column(DateTime(timezone=True), server_default=func.now())
