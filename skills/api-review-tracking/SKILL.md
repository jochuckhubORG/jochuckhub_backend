---
name: api-review-tracking
description: Review this project's Spring Boot APIs one controller at a time, explain the code flow, record only substantiated improvement points, and maintain the API review state in AGENTS.md. Use when the user asks to review an API or to continue, confirm, or track API review progress.
---

# API Review Tracking

1. Read the API review status table in the repository-root `AGENTS.md` first.
2. Review one controller at a time unless the user explicitly requests a different scope.
3. For every endpoint, report:
   - controller and method location;
   - user-facing function;
   - concrete code-reading order from controller through service, repository, entity, DTO, and security where relevant;
   - brief review finding only when supported by the code.
4. Do not invent an improvement point. Distinguish an implementation defect from a product-policy decision.
5. Update `AGENTS.md` immediately after delivering a controller review:
   - move the controller from the not-reviewed state to the reviewed-pending-user state;
   - move it to the user-confirmed state only when the user explicitly closes that controller section (for example, “OK” or “finished here”).
6. Keep endpoint counts and status rows accurate when APIs are added, removed, or moved.

## Status meanings

- Not reviewed: no controller-level review has been delivered.
- Reviewed, pending user confirmation: Codex delivered the review; the user has not accepted or closed it.
- User review complete: the user explicitly reviewed and closed the controller section.
