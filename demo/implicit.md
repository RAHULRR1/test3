Prerequisites Setup---------------

Angular App (Client):
Running on: https://example.com
Redirect URI: https://example.com/callback

Authorization Server:
Running on: https://auth.example.com
Authorization endpoint: https://auth.example.com/oauth/authorize

Backend API (Spring Boot):
Running on: https://api.example.com
Protected endpoints require valid access token




Step 1: User Visits Angular App---------
User action: Navigates to https://example.com

Angular check for any existing token, if a valid token already exists app continue.
If token not present -> Angular redirect user to authorization server with following data

1. response_type=token - Tells auth server to use implicit flow (return token directly)
2. client_id=angular-spa-client  - Identifies your Angular application
3. redirect_uri=https://example.com/callback - Where to send user after login
4. scope=read write profile - What permissions the app needs
5. state=xyz123random - Random value to prevent CSRF attacks (Angular generates and stores this)
6. nonce=abc456random - Random value to prevent replay attacks (optional but recommended)







## Step 2: Authorization Server Receives Request-------------------

Authorization server validates:
1. client_id exists and is registered
2. redirect_uri matches one of the registered URIs for this client
3. response_type is supported
4. scope values are valid and allowed for this client

 -> If validation fails:
- Shows error page to user
- Does NOT redirect back (security measure)

 -> If validation succeeds:
- Shows login page to user at `https://auth.example.com/login'




Step 3: User Logs In----------------------------

 User enters username and password on authorization server's login page

Authorization server:*
1. Validates credentials
2. May show consent screen asking: "Angular App wants to access your profile and data. Allow?"




Step 5: Authorization Server Redirects Back-------------------

Authorization server redirects to:
https://example.com/callback#
  access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...&
  token_type=Bearer&
  expires_in=3600&
  scope=read write profile&
  state=xyz123random



Response parameters:

access_token - The actual token to use for API calls
token_type=Bearer - How to use the token (in Authorization header)
expires_in=3600 - Token valid for 3600 seconds (1 hour)
scope - Confirmed scopes granted
state - Must match the state Angular sent (CSRF protection)




Step 5:  Angular validate the state and store the access token if valid-------------------

if state is invalid then its a CSRF attack-> show unauthorized

if state is valid and token details are valid then store the access token and use it for API call.





 








