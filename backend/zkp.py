import hashlib
from ecdsa import SECP256k1, VerifyingKey, BadSignatureError
from ecdsa.ellipticcurve import Point
from ecdsa.util import sigdecode_string
import binascii

# Curve parameters
curve = SECP256k1
generator = curve.generator
order = curve.order

def sha256(data: bytes) -> bytes:
    return hashlib.sha256(data).digest()

def hex_to_int(hex_str: str) -> int:
    return int(hex_str, 16)

def int_to_hex(val: int) -> str:
    return format(val, '064x')

def point_to_hex(point: Point) -> str:
    # Compress point: 02/03 + x
    # But for simplicity and matching some libs, let's see what format we need.
    # The Kotlin code uses `point.getEncoded(true)` which is compressed format.
    # ecdsa lib handles this.
    return binascii.hexlify(point.to_bytes(encoding="compressed")).decode('utf-8')

def hex_to_point(hex_str: str) -> Point:
    try:
        vk = VerifyingKey.from_string(binascii.unhexlify(hex_str), curve=curve)
        return vk.pubkey.point
    except Exception as e:
        raise ValueError(f"Invalid point hex: {hex_str}") from e

def compute_challenge(R: Point, P: Point, message: str) -> int:
    """
    Compute challenge c = Hash(R || P || message)
    """
    digest = hashlib.sha256()
    
    # R (compressed)
    r_bytes = R.to_bytes(encoding="compressed")
    digest.update(r_bytes)
    
    # P (compressed)
    p_bytes = P.to_bytes(encoding="compressed")
    digest.update(p_bytes)
    
    # Message
    if message:
        digest.update(message.encode('utf-8'))
        
    hash_bytes = digest.digest()
    return int.from_bytes(hash_bytes, byteorder='big') % order

def verify_proof(public_key_hex: str, proof: dict, message: str) -> bool:
    """
    Verify Schnorr Proof
    """
    try:
        # 1. Parse inputs
        P = hex_to_point(public_key_hex)
        R = hex_to_point(proof['commitmentR'])
        c_claimed = hex_to_int(proof['challenge'])
        s = hex_to_int(proof['response'])
        
        # 2. Recompute challenge
        c_computed = compute_challenge(R, P, message)
        
        # 3. Check challenge equality
        if c_claimed != c_computed:
            print(f"Challenge mismatch: claimed={c_claimed}, computed={c_computed}")
            return False
            
        # 4. Verify equation: s*G == R + c*P
        # left = s*G
        left = generator * s
        
        # right = R + c*P
        right = R + (P * c_claimed)
        
        if left == right:
            return True
        else:
            print("Equation mismatch")
            return False
            
    except Exception as e:
        print(f"Verification error: {e}")
        return False
