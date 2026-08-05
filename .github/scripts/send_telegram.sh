#!/usr/bin/env bash
set -e

if [ -z "$TELEGRAM_BOT_TOKEN" ] || [ -z "$TELEGRAM_CHAT_ID" ]; then
  echo "⚠️ Warning: TELEGRAM_BOT_TOKEN or TELEGRAM_CHAT_ID is missing from Repository Secrets!"
  echo "Please configure TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID in GitHub repository Settings -> Secrets and variables -> Actions."
  exit 0
fi

if [ -z "$APK_PATH" ] || [ ! -f "$APK_PATH" ]; then
  echo "❌ Error: APK_PATH is missing or file does not exist!"
  exit 1
fi

COMMIT_HASH=$(git rev-parse --short HEAD 2>/dev/null || echo "N/A")
COMMIT_AUTHOR=$(git log -1 --pretty=format:"%an" 2>/dev/null || echo "Unknown")
COMMIT_TITLE=$(git log -1 --pretty=format:"%s" 2>/dev/null || echo "Update")
COMMIT_BODY=$(git log -1 --pretty=format:"%b" 2>/dev/null | grep -v '^$' || true)

CHANGES="• $COMMIT_TITLE"
if [ -n "$COMMIT_BODY" ]; then
  CHANGES="$CHANGES
$COMMIT_BODY"
fi

CAPTION="🚀 *نسخه جدید برنامه آماده شد (arm64-v8a)*

📱 *برنامه:* حسابداری شخصی
🏗 *معماری:* ARM64 (بهینه‌شده و کم‌حجم)
👤 *توسعه‌دهنده:* $COMMIT_AUTHOR
🔖 *کامیت:* \`$COMMIT_HASH\`

📝 *لیست تغییرات:*
$CHANGES"

curl -s -X POST "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendDocument" \
     -F "chat_id=${TELEGRAM_CHAT_ID}" \
     -F "document=@${APK_PATH}" \
     -F "caption=${CAPTION}" \
     -F "parse_mode=Markdown"
