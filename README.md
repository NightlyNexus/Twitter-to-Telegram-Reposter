Reposts Tweets to a Telegram channel.
====================

Setup:
1. Generate a Twitter bearer token. You will need to create a Twitter developer account to get your API key and your API secret. Receive your bearer token from `curl -u '[API key]:[API secret]' --data 'grant_type=client_credentials' 'https://api.twitter.com/oauth2/token'`
2. Create a Telegram channel.
3. Create a Telegram bot by DMing @bot on Telegram and receive the bot's API key.
4. Make the new bot an administrator of the new Telegram channel.
5. Create src/main/resources/properties.json in this project and fill in the following values:
```json
{
  "twitter_bearer_token": "",
  "twitter_handle": "",
  "telegram_api_key": "",
  "telegram_chat_id": ""
}
```
6. Create src/main/resources/twitter_since_id and enter the latest Twitter post id. Tweets after this id will be reposted to Telegram. After running the program, this id will be set to the last Tweet reposted to Telegram, so the program can continue to run without any further configuration.

License
-------

    Copyright 2019 Eric Cochran

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
