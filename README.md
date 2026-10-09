# NeonText

**Klientowy silnik animowanego tekstu do Minecrafta 26.3 (Fabric).** Twój nick, ranga, lista
graczy, czat i hologramy mogą się animować — tęczowo, płonąć, migać neonem, skakać, „glitchować"
albo wyglądać jak terminal Matrix. Wszystko po stronie klienta, bez serwera i bez pluginów.
**Bez GUI — same komendy**, plus klikanie w hologramy i tekst na czacie.

![NeonText](src/client/resources/assets/neontext/icon.png)

## Funkcje

- **28 efektów animacji** — Rainbow, Gradient, Wave, Bounce, Pulse, Flicker, Glitch, Slide, Spin,
  Fade, Strobe, Static, Fire, Ice, Gold, Matrix, Vaporwave, Glow, Rainbow Bounce, Bubble, Scramble,
  Heartbeat, Candy, Invert, Jelly, Lightning, Aurora…
- **Animowany nick i ranga** — tablice z imionami (nameplates) i lista graczy (TAB) z własnym
  stylem; opcja „tylko moja nazwa", żeby nie ruszać nicków innych graczy.
- **5 niezależnych celów** — Nameplates, Tab list, Chat, Hologramy, GUI & HUD — każdy z własnym
  efektem, paletą, prędkością, amplitudą, wielkością, poświatą i cieniem.
- **Klikanie = edycja** — kliknij hologram na świecie (celownik) albo tekst na czacie, a mod go
  zaznaczy i od razu zamieni na kolejny preset. To samo da się zrobić komendą `/neon select`.
- **Hologramy w świecie** — teksty widoczne tylko dla Ciebie: dodawane komendą, z własnym stylem
  animacji, skalą i zasięgiem.
- **Tylko komendy** — pełna obsługa z czatu: presety (Rainbow Flex, Vaporwave, Owner, Admin, VIP,
  YouTube…), edycja palety, suwaki parametrów, przełączniki — `/neon help` pokazuje wszystko.
- **Konfiguracja w `config/neontext.json`** — czytelny JSON, ręczna edycja bez ryzyka crasha.
- **`/neon debug`** — liczniki diagnostyczne, gdy coś wygląda nie tak.

## Wymagania

- Minecraft **26.3**
- [Fabric Loader](https://fabricmc.net/use/installer/) **0.19.5+**
- [Fabric API](https://modrinth.com/mod/fabric-api) dla 26.3
- Java **25** (tylko do budowania)

## Instalacja

1. Zainstaluj Fabric Loader dla Minecrafta 26.3.
2. Wrzuć `neontext-1.0.0.jar` oraz `fabric-api` do folderu `mods/`.
3. Wejdź na serwer / do świata i wpisz `/neon`.

## Budowanie

```bash
./gradlew build
# gotowy mod: build/libs/neontext-1.0.0.jar
```

Każdy push przechodzi też przez CI (GitHub Actions) — gotowy `.jar` znajdziesz w zakładce
**Actions** (artefakt `neontext`) albo w **Releases** (asset `neontext-latest`).

## Komendy

Wszystko obraca się wokół **zaznaczenia**: klikasz hologram / tekst na czacie (albo wpisujesz
`/neon select`), a potem edytujesz zaznaczony obiekt.

| Komenda | Opis |
| --- | --- |
| `/neon` | status: włączone efekty i aktualne zaznaczenie |
| `/neon help` | lista komend |
| `/neon select <cel\|id>` | zaznacz: `nameplate`, `tab`, `chat`, `holograms`, `gui`, `none` albo id hologramu |
| `/neon select` | zaznacz hologram pod celownikiem |
| `/neon next` | zamienia zaznaczony obiekt na kolejny preset |
| `/neon effect <efekt>` | ustawia efekt zaznaczenia (np. `/neon effect rainbow_bounce`) |
| `/neon preset <preset>` | aplikuje preset (np. `/neon preset Owner`) |
| `/neon speed\|amplitude\|spread\|size\|saturation\|brightness <0-10>` | suwaki parametrów |
| `/neon shadow\|glow\|onlyme <on\|off>` | przełączniki stylu |
| `/neon palette rainbow` / `/neon palette #FF0000 #00FF00` | paleta kolorów |
| `/neon on\|off\|reset\|toggle` | włącza/wyłącza zaznaczenie, resetuje styl, master switch |
| `/neon click <on\|off>` | klikanie = zaznaczanie i zamiana |
| `/neon holo here <id> <tekst>` | hologram w Twojej pozycji |
| `/neon holo add / remove / list / text / move / scale / distance` | zarządzanie hologramami |
| `/neon reload` / `/neon debug` / `/neon creators` | config / diagnostyka / twórcy |

## Struktura

- `src/client/java/pl/neontext/client/anim` — silnik efektów (czysta matematyka + rendering glifów)
- `src/client/java/pl/neontext/client/holo` — hologramy po stronie klienta
- `src/client/java/pl/neontext/client/mixin` — haki w pipeline tekstu (font, HUD, chat, tab, klikanie)
- `src/client/java/pl/neontext/client/core` — runtime, kontekst tekstu, komendy, klikanie
- `src/client/java/pl/neontext/client/cfg` — konfiguracja i presety

## Twórcy

- **Tymoteusz** ([@tymateuszwierzba-ux](https://github.com/tymateuszwierzba-ux)) — Owner & Lead Developer
- **Oskar** ([@Oskarko121](https://github.com/Oskarko121)) — Helper
- `/neon creators` wyświetla twórców w grze.

## Licencja

