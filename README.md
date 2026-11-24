# AutoSell Mods (Fabric 1.21.4)

Dodatek do AutoSell: **Gencash Miner** (3x3/5x5/7x7/9x9/10x10, delay, zasięg, AFK), **AutoFood**,
**ESP 3D** (rudy i gracze, pełne wypełnienie), **tracery od celownika**, **nametagi 3D**, animowany ekran.
Klawisz: **Right Ctrl**. Config: `config/autosell-modules.json`.

## Jak zrobić .jar na GitHubie
1. Utwórz nowe repo na GitHubie i wrzuć CAŁĄ zawartość tego folderu (razem z ukrytym `.github`).
2. Wejdź w zakładkę **Actions** -> workflow **Build jar** (odpali się sam po pushu; albo "Run workflow").
3. Po ~3-5 min kliknij ukończony run -> na dole **Artifacts** -> `autosell-mods-jar` (zip z jarem).
4. Rozpakuj, wrzuć `autosell-mods-1.0.0.jar` do `mods/` razem ze starym `autosell-2.0.0.jar` + Fabric API.

Kod nie był kompilowany przez autora paczki – jeśli build się wywali, skopiuj log z Actions i wklej do Claude.
