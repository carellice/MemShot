#!/bin/zsh
# Doppio click per pubblicare: compila l'APK, lo carica nelle Releases di GitHub
# e cancella le versioni precedenti. La versione sale da sola a ogni pubblicazione.
# L'app trova la nuova versione da Impostazioni → Aggiornamenti.
set -e
set -o pipefail
cd "$(dirname "$0")"

pause() { [ -t 0 ] && { echo; read -k 1 "?Premi un tasto per chiudere."; }; return 0; }
fail() { echo; echo "ERRORE: $1"; pause; exit 1; }
trap 'fail "qualcosa è andato storto: leggi i messaggi qui sopra."' ERR

export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"
command -v gh >/dev/null || fail "manca GitHub CLI (brew install gh)."
gh auth status >/dev/null 2>&1 || fail "GitHub CLI non è collegato: esegui 'gh auth login'."
if [ -z "$JAVA_HOME" ] && [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
fi
[ -f local.properties ] || echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties

source version.properties
cp version.properties version.properties.bak
restore() { mv -f version.properties.bak version.properties 2>/dev/null || true; }

# Si sale di versione solo se quella attuale è già stata pubblicata.
if gh release view "v$VERSION_NAME" >/dev/null 2>&1; then
  VERSION_CODE=$((VERSION_CODE + 1))
  VERSION_NAME="${VERSION_NAME%.*}.$(( ${VERSION_NAME##*.} + 1 ))"
  printf 'VERSION_CODE=%s\nVERSION_NAME=%s\n' "$VERSION_CODE" "$VERSION_NAME" > version.properties
fi
TAG="v$VERSION_NAME"
APK="build/MemShot-$TAG.apk"

echo "==> Compilo MemShot $TAG (build $VERSION_CODE)…"
./gradlew :app:testDebugUnitTest :app:assembleRelease -q || { restore; fail "la compilazione o i test non sono riusciti: la versione non è stata cambiata."; }
mkdir -p build
# APK per telefoni (arm64): il più leggero
cp app/build/outputs/apk/release/app-arm64-v8a-release.apk "$APK"
cp "$APK" MemShot.apk
rm -f version.properties.bak

echo "==> Salvo le modifiche su GitHub…"
git add -A
git diff --cached --quiet || git commit -q -m "Release $TAG"
git push -q origin HEAD

echo "==> Cancello le release precedenti…"
for old in $(gh release list --limit 200 --json tagName -q '.[].tagName'); do
  gh release delete "$old" --cleanup-tag --yes
  echo "    rimossa $old"
done

echo "==> Pubblico $TAG…"
gh release create "$TAG" "$APK" --title "MemShot $TAG" --target "$(git rev-parse HEAD)" \
  --notes "APK di MemShot $TAG. Se hai già MemShot, aggiorna da Impostazioni → Aggiornamenti. Altrimenti scarica il file sul telefono Android e aprilo per installarlo (va consentita l'installazione da origini sconosciute)."

echo
echo "Fatto! $(gh release view "$TAG" --json url -q .url)"
pause
