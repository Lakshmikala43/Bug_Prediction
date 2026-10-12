from sqlalchemy import Column, Integer, String, Boolean, ForeignKey, DateTime, Text
from sqlalchemy.orm import relationship
import datetime
from database import Base

class User(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, unique=True, index=True)
    password_hash = Column(String) # Simulated hash
    role_id = Column(Integer, ForeignKey("roles.id"))
    is_active = Column(Boolean, default=True)
    role = relationship("Role")

class Role(Base):
    __tablename__ = "roles"
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, unique=True) # Administrator, Security Analyst, Cloud Engineer, Auditor
    description = Column(String)

class CloudEnvironment(Base):
    __tablename__ = "environments"
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, unique=True)
    description = Column(String)

class CloudResource(Base):
    __tablename__ = "cloud_resources"
    id = Column(Integer, primary_key=True, index=True)
    resource_id = Column(String, unique=True, index=True)
    name = Column(String)
    resource_type = Column(String)
    environment_id = Column(Integer, ForeignKey("environments.id"))
    owner = Column(String)
    status = Column(String, default="Active")
    configuration = Column(Text) # JSON string simulation
    
    environment = relationship("CloudEnvironment")

class SecurityPolicy(Base):
    __tablename__ = "security_policies"
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String)
    description = Column(String)
    severity = Column(String)
    remediation_guidance = Column(Text)
    is_active = Column(Boolean, default=True)

class Finding(Base):
    __tablename__ = "findings"
    id = Column(Integer, primary_key=True, index=True)
    resource_id = Column(Integer, ForeignKey("cloud_resources.id"))
    policy_id = Column(Integer, ForeignKey("security_policies.id"))
    description = Column(String)
    severity = Column(String)
    risk_score = Column(Integer)
    status = Column(String, default="Open")
    created_at = Column(DateTime, default=datetime.datetime.utcnow)
    
    resource = relationship("CloudResource")
    policy = relationship("SecurityPolicy")

class Alert(Base):
    __tablename__ = "alerts"
    id = Column(Integer, primary_key=True, index=True)
    finding_id = Column(Integer, ForeignKey("findings.id"))
    status = Column(String, default="New") # New, Acknowledged, Investigating, Resolved
    assigned_to = Column(Integer, ForeignKey("users.id"), nullable=True)
    created_at = Column(DateTime, default=datetime.datetime.utcnow)
    
    finding = relationship("Finding")
    assignee = relationship("User")

class AuditLog(Base):
    __tablename__ = "audit_logs"
    id = Column(Integer, primary_key=True, index=True)
    user_email = Column(String)
    action = Column(String)
    resource = Column(String)
    timestamp = Column(DateTime, default=datetime.datetime.utcnow)
    result = Column(String)