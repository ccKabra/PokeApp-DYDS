# PokeApp

Juego de cartas coleccionables Pokémon de escritorio
que combina **cuatro APIs públicas en tiempo real**. Tiene batallas donde el
**clima real del mundo** afecta el resultado, una campaña de **gimnasios y ligas
a través de 3 regiones**, economía con tienda e items equipables, y un modo
**roguelite aparte**

---

## APIs y cómo interactúan

```
        ┌──────────────────────────────────────────────┐
        │             PokemonDetailBroker               │
        │  (PokemonDetailExternalSource: combina las    │
        │   dos fuentes de datos + imagen)              │
        └──────────┬─────────────────────┬──────────────┘
                   │                     │
        ┌──────────▼──────┐   ┌──────────▼──────────┐
        │  PokeApiSource  │   │   TcgExternalSource │
        │   (source_a)    │   │     (source_b)      │
        │   pokeapi.co    │   │   pokemontcg.io     │
        └─────────────────┘   └─────────────────────┘

        ┌──────────────────────────────────────────────┐
        │           CardsSourceWithFallback             │
        │  (PokemonCardsExternalSource: cartas TCG)     │
        │   primaria: TcgDex  →  secundaria: Tcg        │
        └──────────┬─────────────────────┬──────────────┘
                   │                     │
        ┌──────────▼──────┐   ┌──────────▼──────────┐
        │ TcgDexSource    │   │   TcgExternalSource │
        │   (source_c)    │   │     (source_b)      │
        │  tcgdex.net     │   │   pokemontcg.io     │
        └─────────────────┘   └─────────────────────┘

        ┌──────────────────────────────────────────────┐
        │                 WeatherSource                 │
        │                open-meteo.com                 │
        └──────────────────────────────────────────────┘
```

| API | URL base | Qué provee |
|-----|----------|-----------|
| **PokéAPI** | `pokeapi.co/api/v2` | Pokémon, stats, tipos, cadena evolutiva, sprites de items, artwork oficial |
| **Pokémon TCG API** | `api.pokemontcg.io/v2` | Imágenes de cartas coleccionables (fuente secundaria) |
| **TCGdex** | `api.tcgdex.net/v2` | Imágenes de cartas coleccionables (fuente primaria, GraphQL) |
| **Open-Meteo** | `api.open-meteo.com/v1` | Clima actual por coordenadas GPS |

Las medallas de gimnasio usan los sprites oficiales del repo de sprites de PokeAPI;
los entrenadores de Liga y el sprite del jugador usan los sprites de Pokémon Showdown.

### Lógica del Broker y del Fallback

El `PokemonDetailBroker` combina PokéAPI + TCG para enriquecer cada Pokémon
(stats/tipos/evoluciones de PokéAPI + imagen de carta de TCG), con fallback:
ambas → merge completo; solo PokéAPI → artwork oficial; solo TCG → datos mínimos;
ninguna → `null` (el repositorio maneja el error).

El `CardsSourceWithFallback` resuelve las **cartas coleccionables** pidiéndolas
primero a **TCGdex** y, si falla o viene vacío, cae a la **Pokémon TCG API** —
una sola interfaz (`PokemonCardsExternalSource`) con dos backends intercambiables.

**WeatherSource** no tiene broker: Open-Meteo es la única fuente de clima, se
consulta una vez por ubicación al cargar la pantalla.

---

## Arquitectura

```
src/main/kotlin/com/pokemonarena/
│
├── core/Constants.kt
│
├── domain/                          ← Sin dependencias externas. Nunca.
│   ├── entity/
│   │   ├── Entities.kt              ← Pokemon, Card, Gym, BattleResult, Stats,
│   │   │                              UserStatistics, WeatherCondition, Winner…
│   │   ├── Region.kt                ← Region (Kanto/Johto/Hoenn), League,
│   │   │                              LeagueOpponent, Progression (desbloqueo)
│   │   ├── BattleModifiers.kt       ← BattleScore (fórmula), RarityBoost, BattleFatigue
│   │   ├── TypeEffectiveness.kt     ← tabla clima × tipo
│   │   ├── TypeMatchup.kt           ← tabla tipo vs tipo
│   │   ├── CardPricing.kt           ← precio dinámico (stats × rareza)
│   │   ├── BattleRewards.kt         ← recompensas + anti-farmeo + bono 1ª victoria
│   │   ├── CoinMine.kt · AimGame.kt ← reglas de los dos minijuegos de la Mina
│   │   ├── Item.kt                  ← items equipables, consumibles y exclusivos
│   │   ├── PlayerProfile.kt         ← perfil (nombre + género)
│   │   └── Rogue*.kt                ← MÓDULO ROGUE (entidades, items, evoluciones,
│   │                                  ataques) — totalmente aparte del juego base
│   ├── repository/Repositories.kt   ← interfaces de repositorio (DIP)
│   └── usecase/                      ← un caso de uso por acción, todos con execute()
│       ├── GetPokemons / GetPokemonDetail / Battle / Card / Gym / Item / Badge
│       ├── Statistics / Team / MineCoins / RegisterAimShot / Profile / CoinTransactions
│       └── Rogue*                    ← pool, motor de combate y cash-out del modo Rogue
│
├── data/
│   ├── external/
│   │   ├── broker/                  ← PokemonDetailBroker + CardsSourceWithFallback
│   │   ├── source_a/ (PokeAPI)  source_b/ (TCG)  source_c/ (TCGdex)
│   │   ├── WeatherSource + WeatherCodeMapper (WMO → WeatherCondition)
│   │   └── ItemsExternalSource      ← sprites de items
│   ├── local/
│   │   ├── database/Database.kt     ← Exposed + SQLite, auto-migración
│   │   └── dao/                     ← FavoriteCard, BattleHistory, UserStatistics,
│   │                                  ItemInventory, GymBadge, UserProfile
│   └── repository/                  ← *RepositoryImpl (Pokemon, Card, Gym, Weather,
│                                       Battle, Item, Badge, Profile, RoguePool)
│
├── di/DependencyInjector.kt         ← DI manual con lazy, sin Koin
│
└── presentation/
    ├── BaseViewModel.kt · UiMessage.kt
    ├── navigation/                  ← Navigator, NavigationSidebar, NavigationViewModel
    ├── theme/                       ← Theme, AppColors, AppIcons (cero emojis)
    ├── utils/                       ← CommonComposables, HeldItemControls,
    │                                  ScoreBreakdownView, BattleSupport, TrainerSprites
    └── screens/
        ├── profile/    ← alta de perfil en el primer arranque
        ├── home/ · collection/ · detail/ · statistics/
        ├── gyms/ · battle/ · result/ · league/   ← campaña por regiones
        ├── myteam/ · items/
        ├── mine/       ← globo anti-autoclicker + minijuego de puntería
        └── rogue/      ← Expedición Rogue (UI propia)
```

---

## Versiones

| Categoría | Tecnología | Versión |
|-----------|-----------|---------|
| Lenguaje | Kotlin | 1.9.23 |
| UI | Compose Multiplatform Desktop | 1.6.2 |
| HTTP | Ktor Client CIO | 2.3.9 |
| Serialización | Kotlinx Serialization JSON | 1.6.3 |
| Base de datos | Exposed + SQLite JDBC | 0.48.0 |
| Imágenes async | Kamel | 0.9.4 |
| Tests | kotlin.test + MockK + Coroutines Test + Turbine | — |
| Build | Gradle 8.6 (Kotlin DSL) | — |
| JVM | Java 17 | — |

---

## Funcionalidades

###  Perfil de entrenador
En el primer arranque elegís **nombre** y **género**; el sprite del entrenador
(Rojo / Hoja) aparece arriba a la izquierda junto a "PokeApp". Se guarda en SQLite
y solo se pide una vez.

###  Dashboard
Estadísticas en tiempo real (batallas, victorias, win rate, racha), vitrina de
medallas, preview del equipo y últimas batallas con su cambio de monedas.

###  Pokédex
Pokémon cargados de una sola vez con filtrado local instantáneo. El catálogo
disponible **crece con tu progreso de región**: Kanto (151) → Johto (251) →
Hoenn (386), a medida que desbloqueás regiones.

###  Detalle + Compra
Artwork oficial + carta TCG (del Broker), stats con barras, cadena evolutiva, y un
grid con **todas las versiones TCG** (rareza, set, precio y BST de combate propios).
Precio dinámico `stats × rareza` (mín. 100). **Colección limitada a 8 cartas**:
con la colección llena hay que vender antes de comprar.

### ⚔ Mi Equipo
Grilla a pantalla completa de 8 slots con BST efectivo (rareza + item + fatiga),
item equipado y venta. Selección de hasta 3 cartas para el equipo de batalla.
**Venta al 50%** castigada por fatiga; **curar fatiga** con Raíz Energía.

### ️ Gimnasios — 24 a través de 3 regiones
8 gimnasios por región (Kanto, Johto, Hoenn) en ciudades reales, cada uno con
**clima en vivo** vía Open-Meteo, pool de 8 Pokémon (3 al azar por desafío, todos
con item) y dificultad 1-5★. Los conquistados muestran su medalla.

| Región | Gimnasios (tipo) |
|--------|------------------|
| **Kanto** | Plateada (rock) · Azulona (water) · Lavapolis (fire) · Lavanda (ghost) · Verdalia (grass) · Voltio (electric) · Psíquico (psychic) · Glaciar (ice) |
| **Johto** | Violeta (flying) · Azalea (bug) · Trigal (normal) · Iris (ghost) · Orquídea (fighting) · Acero (steel) · Escarcha (ice) · Dragón (dragon) |
| **Hoenn** | Férrico (rock) · Puño (fighting) · Dínamo (electric) · Caldera (fire) · Equilibrio (normal) · Pluma (flying) · Mental (psychic) · Marea (water) |

### 🏆 Liga Pokémon
Una Liga por región: **4 entrenadores consecutivos** (estilo Alto Mando). Entre
combates reorganizás el equipo, equipás items o curás fatiga — pero **no podés
comprar**. Si perdés uno, el desafío termina. Conquistarla por primera vez da la
medalla de Liga + un **item exclusivo**. El flujo es *pelear → armar → pelear*.

###  Progresión por regiones
Para desbloquear una región hay que **conquistar todos los gimnasios + la Liga**
de la anterior (`Progression`). Esto también amplía el Pokédex y sube la dificultad
base de los rivales (`RegionDifficulty`).

### ️ Batalla clásica — al mejor de 3 rondas
Fase de estrategia (ordenás los cruces con hints de tipo/clima; 45% de chance de
que se te caiga un item al entrar) + fase de combate animada ronda a ronda:

```
score          = (statsEfectivas ponderadas / 255) × clima[tipo] × matchup × crítico
statsEfectivas = base × item × bonusRareza × fatiga
ponderación    = atk×0.25 + def×0.20 + spd×0.20 + hp×0.15 + spAtk×0.10 + spDef×0.10
matchup        = ×1.25 ventaja · ×0.8 desventaja
crítico        = ×1.5  (10% jugador, 20% gimnasio)
miss           = 30% (solo el jugador; la Lupa lo baja)
```

Cada ronda tiene un **desglose expandible** que muestra de dónde sale cada número.

**Economía anti-farmeo:**
```
Victoria: (35 + 45×dificultad) × factorJusticia   (aplastar paga solo el 40%)
Derrota: -(10 + 5×dificultad)  ·  Empate: +10×dificultad
Primera victoria en un gimnasio: medalla + bono de monedas (una sola vez)
```

###  Tienda de Items
**Equipables** (boostean stats o precisión): Cinta/Gafas/Pañuelo Elección,
Chaleco Asalto, Mineral Evolutivo, Vidasfera, Lupa. **Consumibles**: Raíz Energía
(elimina la fatiga). **Exclusivos** (no se compran, solo se ganan en gimnasios/ligas):
Cinto Experto, Cinta Fuerte, Telescopio. Se equipan por carta en Mi Equipo.

###  Mina de Monedas — dos minijuegos
1. **Globo anti-autoclicker**: cada golpe paga según una tabla de probabilidades
   (55% nada … 0.5% jackpot de +100) e infla un globo; si explota hay cooldown.
   Un macro de clicks constantes revienta siempre; un humano, no.
2. **Puntería**: 3 globos que se mueven y rebotan en un rectángulo; pegarles paga
   (más chico = más caro), errar al fondo descuenta, y si no los reventás en 3s
   explotan solos. Mini reflejos puros.

### 💀 Expedición Rogue — el "Combate Imposible"
Un modo **roguelite totalmente aparte** (entidades, motor de combate y economía
propios; no toca tu colección ni tu equipo). Diseñado para ser **imposible de
ganar por diseño** — emula la frustración de una batalla amañada — pero profundo
y estratégico:

- **Draft**: elegís 1 de 3 iniciales al azar y subís **12 pisos**.
- **Caminos**: en cada piso elegís entre Combate, Élite, Descanso, Tesoro o **Dojo**
  (entrenamiento que da mucha XP y cura).
- **Combate por turnos con HP**: elegís **ataque** (tipo + STAB importan), cambiás
  de Pokémon (cuesta el turno), con animaciones de embestida, números de daño y
  barras de HP. El HP persiste entre combates; un debilitado no vuelve.
- **Crecimiento real**: los Pokémon ganan XP, **suben de nivel**, **aprenden
  ataques** y **evolucionan** (Oddish→Gloom→Vileplume…). Los reclutas llegan ya
  evolucionados acorde a tu nivel.
- **Recompensas**: reclutar (hasta 3), curar, **bendiciones** pasivas (Furia,
  Aguante, Ímpetu, Vampirismo, Fortuna) o **items equipables** (boostean ATQ/DEF/VEL/HP).
- **Armadura Argumental**: el rival **nunca baja de 1 HP**; todo golpe letal lo
  enfurece (se cura, se potencia y te suelta una burla) y además escala cada turno.
  No existe rama de victoria: el KO es estructuralmente inalcanzable.
- **Sobrevivir y huir**: no se gana, pero aguantando 6 turnos podés **escapar** al
  siguiente piso (o gastar una de tus 3 **Fichas de Esperanza** para huir antes).
- **Jefe final**: legendario, **no se puede vencer ni huir**. La derrota es inevitable.
- Cobrás la mitad del botín juntado, ganes o no (nunca perdés monedas).

> La imposibilidad de ganar es **intencional y está documentada** en `RogueRules`,
> y cubierta por tests. Es reversible por diseño (no hay un flag mágico).

---

## Tests

Cobertura amplia con `kotlin.test` + **MockK** + **Fakes propios** + Coroutines Test.

| Área | Archivos |
|------|----------|
| **Fuentes / Broker** | `PokemonDetailBrokerTest`, `CardsSourceWithFallbackTest`, `WeatherCodeMapperTest` |
| **Reglas de dominio** | `GameRulesTest`, `WeatherConditionTest`, `SimulateBattleUseCaseTest`, `EconomyUseCasesTest`, `MineCoinsUseCaseTest`, `RegisterAimShotUseCaseTest` |
| **Use cases / repos** | `UseCasesTest`, `ItemUseCasesTest`, `UpdateStatisticsAfterBattleUseCaseTest`, `RepositoryImplTest`, `GymRepositoryImplTest`, `CardRepositoryImplTest` |
| **ViewModels** | `ViewModelsTest`, `NewViewModelsTest`, `CardDetailViewModelTest`, `CollectionViewModelTest`, `GymsViewModelTest`, `StatisticsViewModelTest`, `MineViewModelTest` |
| **Modo Rogue** | `RogueBattleEngineTest`, `RogueRulesTest`, `RogueProgressionTest`, `RogueItemTest`, `RogueEvolutionTest`, `RogueViewModelTest` |

Los tests del modo Rogue incluyen **garantías de que la victoria es imposible**
(el rival nunca cae, atacar para siempre termina en derrota, el jefe no se puede
huir ni vencer).

---

## Persistencia de datos

SQLite en `~/.pokemonarena/pokemonarena.db`, auto-migrada con Exposed:

| Tabla | Datos |
|-------|-------|
| `favorite_cards` | Cartas compradas con stats, `in_team`, `held_item`, `times_used` (fatiga) |
| `battle_history` | Resultado, scores, clima, fecha ISO, `coins_delta` |
| `user_statistics` | Totales, rachas, Pokémon favorito, monedas |
| `item_inventory` | Items comprados con su cantidad |
| `gym_badges` | Gimnasios y ligas conquistados, con fecha |
| `user_profile` | Nombre y género del entrenador |

> La **Expedición Rogue** no se persiste: cada run vive en memoria y se cobra al
> final, por diseño.

---