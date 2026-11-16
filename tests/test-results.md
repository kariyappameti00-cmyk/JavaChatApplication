# JavaChatApplication - Functional Test Results

## Test Environment
- Operating System: macOS
- Java Version: 27.0.1
- Application: Java Chat Application
- Server: ChatServer
- GUI: LoginFrame

## Functional Test Cases

| ID | Test Case | Expected Result | Actual Result | Status |
|----|-----------|-----------------|---------------|--------|
| T01 | Server startup | Server starts successfully | Server started | PASS |
| T02 | Client connection | Client connects to server | Multiple clients connected | PASS |
| T03 | User registration | New account is created | New user registered successfully | PASS |
| T04 | Valid login | User logs in successfully | KP3 logged in successfully | PASS |
| T05 | Invalid login | Invalid credentials are rejected | Incorrect password rejected successfully | PASS |
| T06 | Send message | Message is sent successfully | Message sent successfully between users | PASS |
| T07 | Receive message | Recipient receives message | Recipient received the message | PASS |
| T08 | Multiple clients | Multiple clients connect | Multiple connections observed | PASS |
| T09 | Message history | Previous messages are available | Previous messages displayed after reopening | PASS |
| T10 | Group request | User can request group access | User successfully submitted group access request | PASS |
| T11 | Admin approval | Admin approved the group request and access was granted | Group request approved successfully | PASS |
| T12 | Server logging | Server events are logged | Connection and login logs observed | PASS |

## Notes
- PASS means the functionality was observed working.
- NOT TESTED means the functionality still requires verification.
- Update actual results after completing each test.
