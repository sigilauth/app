# iOS App Blockers

**Critical path blockers** preventing implementation of production code.

---

## ✅ RESOLVED: B0 (OpenAPI Spec)

**Owner:** @echo  
**Status:** ✅ DELIVERED (2026-04-23)  
**Location:** `/Volumes/Expansion/src/sigilauth/api/openapi.yaml`  
**Unblocked:**
- Network layer implementation
- API client code generation
- Challenge request/response types
- MPA request/response types
- Secure decrypt request/response types

**Next:** Begin network layer implementation (Priority 2)

---

## HIGH: Protocol-Spec §11 Test Vectors

**Owner:** @knox + @beacon  
**Status:** Placeholder section exists, no actual test vectors  
**Blocking:**
- Crypto implementation validation
- HKDF key derivation tests
- ECDSA signature verification tests
- Pictogram derivation validation
- BIP39 mnemonic tests

**Impact:** Cannot validate crypto implementation correctness.

**Workaround:** Generate own test vectors using reference implementation (e.g., @noble/curves), validate with knox before merging.

---

## MEDIUM: B2 (Relay) for Push Testing

**Owner:** Second Go agent (parallel with B1)  
**Status:** In progress  
**Blocking:**
- APNs push notification end-to-end testing
- Push token registration testing
- Challenge delivery via push

**Impact:** Cannot test push notification flow end-to-end.

**Workaround:** Test with local mock relay, WebSocket fallback for challenges.

---

## MEDIUM: Fluent P1-P6 Locales

**Owner:** @suki + @cora  
**Status:** English complete, 6 P0 locales in progress  
**Blocking:**
- P0 launch in non-English locales
- RTL layout testing (ar, he, fa, ur)

**Impact:** Cannot ship to non-English users.

**Workaround:** English-only for initial testing. P1-P6 locales can be added post-MVP.

---

## LOW: Design Tokens Finalization

**Owner:** @iris  
**Status:** Design direction spec complete, tokens need export  
**Blocking:**
- Exact color values
- Spacing scale confirmation
- Shadow tokens
- Typography scale

**Impact:** Minor — currently using placeholder values from spec.

**Workaround:** Using values from iris-design-direction.md §1 as placeholders.

---

## Resolution Plan

1. **Immediate (today):** Continue scaffolding, create mock API client types
2. **Week 1:** Implement crypto core (independent of B0), validate with knox
3. **Week 1-2:** Implement Secure Enclave keychain (independent of B0)
4. **After B0 delivery:** Implement network layer, replace mock types
5. **After B2 delivery:** Integration test push notifications

**Estimated unblocking timeline:** 1-2 weeks (dependent on B0 + B2 delivery)

---

**Last updated:** 2026-04-23  
**Owner:** @nova
