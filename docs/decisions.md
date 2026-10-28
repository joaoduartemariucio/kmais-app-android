# Decisions

Short record of choices that are not obvious from the code. Each one says what was decided,
why, and what it costs. Newest first within each group. Product decisions are shared with
[kmais-app-ios](https://github.com/joaoduartemariucio/kmais-app-ios); entries marked
**Android** record where this app had to differ.

## Platform parity

### Android: the app is Rodaro, `br.com.joaoduartemariucio.rodaro`

The app was renamed from KMais to Rodaro. The package, `namespace` and `applicationId` all became
`br.com.joaoduartemariucio.rodaro`, and the `Kmais…` types became `Rodaro…` (`RodaroDatabase`,
`RodaroApplication`, `RodaroTheme`, `Theme.Rodaro`). With no `_` in the package, ktlint's
`package-name` rule is back on.

- **Cost:** a new `applicationId` is a new app for Android and the Play Store. An install of the
  old id keeps its data and is not upgraded; the database file is now `rodaro.db`.
- **Not renamed:** the repository (`kmais-app-android`, `rootProject.name`) and the links to
  `kmais-app-ios`, which follow the GitHub repository names.

### Android: launcher icon from a bitmap

The icon is the teal "R" with a road on `#0E131D`. The adaptive icon uses PNG layers per
density (`mipmap-*/ic_launcher_foreground.png`, `ic_launcher_monochrome.png` for themed icons)
over the `ic_launcher_background` color, with the logo inside the 66 dp safe zone. The
notification small icon (`drawable-*/ic_notification.png`) is the same silhouette in white.
Legacy `ic_launcher.webp` files were removed: with `minSdk` 36 the adaptive icon is always used.

- **Revisit when:** a vector version of the logo exists — it would replace the PNGs.

### One app, two native codebases, same rules

The Android app is a port of the iOS app, not a reinterpretation: the same screens, the same
Domain rules with the same test cases, the same strings in English and pt-BR, the same agents
and the same CI gates. Platform idioms win only where copying iOS would feel wrong on Android
(sheets become full-screen dialogs, swipe actions and long-press menus replace context menus,
Material 3 components replace SwiftUI's).

- **Why:** the domain is the product; two apps that disagree on when an oil change is due
  would be worse than one app.
- **Cost:** every rule change lands twice. The shared test names make the diff between the two
  test suites the checklist.

### Android: minimum SDK 36

The app targets the newest platform only, like the iOS app targets iOS 26.5.

- **Why:** one developer; no compatibility code for notification permission, edge-to-edge or
  per-app languages.
- **Revisit when:** the app is published to people on older phones.

## Data entry

### Odometer out of order is a warning, not an error

When the odometer typed in a fuel-up or service does not fit between the previous and the
next record (by date), the form shows an orange note — "the next record has 56,000 km;
saving this changes the vehicle's history" — and still lets the user save.

- **Why:** people log old receipts, fix typos and register a car after the fact. Blocking
  the save made editing old records impossible.
- **Cost:** the history can hold inconsistent readings. Domain already copes:
  `Consumption.anomalies(records)` flags them and they are left out of consumption stats.

### Numeric fields are digit-shifting masks

Typing fills from the right, like banking apps: `5899` becomes `R$ 5,899`. Each field has
a `NumberInput.Style`:

| Style | Fraction digits | Used for |
|---|---|---|
| `KILOMETERS`, `MONTHS` | 0 | odometer, rule intervals |
| `LITERS` | 2 | liters, tank capacity |
| `FUEL_PRICE` | 3 | price per liter (pumps in Brazil charge in 3 decimals) |
| `MONEY` | 2 | service cost |

- **Why:** a plain number pad is enough (no decimal separator to type or get wrong), the
  value is always shown formatted for the locale, and parsing is just
  `digits / 10^fractionDigits` — identical in any locale.
- **Cost:** the caret always sits at the end; editing in the middle of a number is not
  supported. Deleting a non-digit (a trailing currency symbol) removes the last digit.
- **Android:** the field uses `KeyboardType.NumberPassword`, the only Android keyboard that is
  digits only on every IME; the text itself is never hidden.

### Keyboard closes on tap outside, scroll or "Done"

`Modifier.dismissesKeyboard()` on each form clears focus when the user taps outside a text
field or drags the list; number fields close it with the keyboard's Done key.

- **Why:** Compose has no "tap outside" of its own. A parent gesture that clears focus also
  fires on taps that land on a field, so text inputs mark the gesture with
  `Modifier.keepsKeyboard()` and the parent skips it — the same rule as the iOS recognizer,
  which ignores touches on `UITextField`.
- **Android:** only drags made with a finger clear focus. Focusing a field under the keyboard
  scrolls it into view, and that automatic scroll must not dismiss the keyboard again.

## Quality gates

### Coverage is checked on changed lines, not on the whole project

CI requires 80% of the executable lines added or modified by a change to run in tests
(see [CI](ci.md#coverage-of-changed-lines)). Compose layout and app wiring are left out via
`.coverageignore`.

- **Why:** a global threshold either blocks everything from day one or is set so low it
  means nothing. Checking new code raises coverage with every change and puts the cost on
  whoever writes the code, while it is still fresh.
- **Cost:** old untested code only gets tests when someone touches it — refactors and even
  reformatting pull those lines into the check. The ignore list needs judgment: a composable
  that grows real logic should move it to its ViewModel rather than stay excluded.
- **Android:** JaCoCo needs `includeNoLocationClasses` to see code run under Robolectric,
  which is how the Room repositories and the scheduler are tested.

### ktlint with community defaults

The formatter runs ktlint's official style through Spotless, with options pinned only where
the defaults clash with the project ([CI](ci.md#format--spotless--ktlint)). Formatting is
checked, never discussed in review.

### Layer rules as a script instead of a lint plugin

**Android:** SwiftLint's custom regex rules have no direct equivalent; a detekt or Android
Lint rule would need its own module. `scripts/layer_rules.py` applies the same three rules
with the same names, ignoring comments and strings.

## Architecture

### Layers as packages in one module, guarded by lint

- **Why:** one developer, one app module. Separate Gradle modules would add build
  configuration for little gain today.
- **Cost:** the compiler can't stop `presentation` from using a `data` type; the layer rule
  does, and its list of names is manual.
- **Revisit when:** a second target (widget, Wear OS, Android Auto) needs the same data —
  then `domain` and `data` become Gradle modules.

### No reactive queries in the UI

- **Why:** collecting Room `Flow`s in composables puts storage types in the UI and erases the
  Data/Presentation boundary — the same reason the iOS app does not use `@Query`.
- **Cost:** ViewModels reload after every write; a change made elsewhere shows up on the
  next load. Fine for a single-user, offline app.
- **Revisit when:** a second writer appears (sync, widget) — an additive `observe…(): Flow`
  method on the repositories fits without changing the existing ones.

### No use-case layer

- **Why:** most use cases would be one-line pass-throughs to a repository. Business rules
  live in `domain/<feature>/calculations` and are tested there.
- **Revisit when:** the same flow is triggered from three or more screens, or one operation
  has to coordinate several repositories consistently.

### Domain and Data grouped by feature

`domain/` and `data/` are split into `vehicles/`, `fueling/`, `maintenance/` and `reminders/`
(plus `costs/` in Domain and `database/` in Data), with `common/` for what every feature
uses. Inside each feature the Clean Architecture folders stay: `entities/`, `repositories/`,
`calculations/`, `services/` in Domain; `models/`, `mappers/`, `repositories/`, `services/` in
Data. The odometer belongs to
`vehicles/` because readings come from both fuel-ups and services; `Cost` gets its own
package for the same reason.

- **Why:** the same split as `presentation/`, so a feature is found by name in every layer,
  and inside it each kind of type still has its usual place.
- **Cost:** Room exports the schema under the database's qualified name, so the folder in
  `app/schemas/` moved to `…data.database.RodaroDatabase`. WorkManager stores the worker's
  class name, so a reminder scheduled before the move fails to start; the next sync replaces
  it (`enqueueUniqueWork` with `REPLACE`).

### One folder per screen in `presentation/`

Each screen gets its own package with its composable, its ViewModel and a `components/`
folder for the rows and cards only it uses: `vehicles/list`, `vehicles/detail`,
`vehicles/form`, `fueling/form`, `maintenance/form`. The fuel and service rows live in
`vehicles/detail/components` because the detail timeline is the only place they show up.
`VehicleStats` stays in `vehicles/` because both the list and the detail use it; code shared
across features stays in `common/`.

- **Why:** finding a screen means opening one folder, and a screen's private pieces are not
  mixed with another screen's.
- **Cost:** a component that starts being reused has to move to the feature root or to
  `common/`.

### Android: Room with foreign keys, sync off

The iOS schema stays CloudKit-ready (no unique constraints, optional relationships, orphans
dropped by the mappers). Room needs a primary key, so ids are unique, and every record
references its vehicle with `ON DELETE CASCADE`, so orphans cannot exist and the mappers have
nothing to drop.

- **Cost:** a future sync layer has to bring its own conflict handling for ids.

### Android: bottom bar with Vehicles, Archived and Profile

Archived moved from a button on the vehicle list to its own tab, next to Vehicles and Profile.
Each tab keeps its own back stack, and the bar stays visible on pushed screens, like a tab
bar on iOS, so the content never changes size during a transition. Switching tabs is
instant, while push and pop inside a tab slide.

- **Cost:** only the routes of the tabs you are not on are kept. Their entries leave the
  `NavDisplay` back stack, so their ViewModels and saved UI state are cleared. Coming back
  reloads the screen; with local data that takes a few milliseconds.
- **Profile** has no account behind it yet: it shows the app's name, purpose and version.

### Android: navigation per feature, one main host, slide transitions

Each feature declares its routes and entries in `presentation/<feature>/navigation`; the
shared `Route` and `Navigator` are in `presentation/common/navigation`; `app/navigation/AppNavHost`
is the single `NavDisplay` that adds every feature's entries. Screens only get callbacks.
A feature's routes are its own business, the same way its screens are — one central file
listing every destination would grow with every feature and mix them together.
Pushes slide the new screen in from the right over the old one, and pops do the reverse, like a
navigation stack on iOS.

- **Why:** the Navigation 3 default is a crossfade. Halfway through it both screens are
  translucent and the window background shows through, which looked like a blink, worst in
  dark mode, where the window theme was still light. Slides keep both screens opaque, the
  window theme now follows the system (`values-night/themes.xml`) and `AppNavHost` draws an
  opaque background under every transition.
- **Also:** `LoadingIndicator` shows the spinner only after 400 ms, so a fast local load never
  flashes a spinner during the transition.
- **Cost:** the back stack lives in `remember`, so a configuration change that recreates the
  activity returns to the vehicle list. Keeping it needs saveable routes (ids instead of the
  `Vehicle` in `VehiclesRoute.VehicleDetail`).

### Android: ViewModels live in the navigation entry; forms live in their dialog

List and detail ViewModels are created with `viewModel { }` inside a Navigation 3 entry, so
they survive rotation and are cleared when the screen leaves the back stack. Form ViewModels
are `remember`ed by their dialog, so each opening starts clean — the iOS `@State` behavior.

- **Cost:** an open form is lost on rotation.

### Android: localized text outside composables goes through `Localizer`

`ServiceReminderSync` builds notification text and `VehicleStats` builds "in 3 days" labels
outside any composable. Both take a `Localizer`: Android resources in the app,
`TestLocalizer` (which reads the real `strings.xml`) in tests. Domain errors carry no text;
`Throwable.message(localizer)` turns them into the copy the iOS `LocalizedError` provides.

## Calculations

### Money is `BigDecimal`; distance and volume are `Double`

Prices and totals never go through floating-point arithmetic. `Double.toDecimal()` converts
through the shortest string form, so `0.1` stays `0.1`. Divisions use `MathContext.DECIMAL128`
and comparisons use `compareTo`, never `equals` (which also compares scale).

### Consumption only between full tanks

A km/L value needs a full tank at both ends; partial fill-ups in between add their liters
to the interval. Entries with non-positive liters are skipped entirely and reported as
anomalies.

### Forecasts are deterministic

`Maintenance.forecast` never reads the clock; the latest reading is "now". The next
service is the earlier of the distance-based date (from the vehicle's km/day pace) and
the calendar-based date. Months use `365.25 / 12` days only when shown to the user.

### Android: calendar math on `java.time` with an explicit zone

Every function that iOS gives a `Calendar` takes a `ZoneId` here. Adding months clamps to
the end of the month exactly like `Calendar.date(byAdding:)`, and day counts compare local
dates, so tests pin `ZoneOffset.UTC` the way the iOS tests pin a UTC calendar.

### The current odometer is the most recent reading, not the highest

A reading that goes backwards is bad data; returning the maximum would hide it.

## Reminders

### Reminders are re-planned from scratch on every sync

`ServiceReminderSync` loads every active vehicle, asks `ReminderPlan` (pure, in `domain`)
when each rule is due, cancels pending notifications that no longer match a rule and
schedules the rest. Each notification's id is its rule's id, so scheduling again replaces it.

- **Why:** due dates move whenever anything changes — a fuel-up shifts the km/month pace, a
  service resets the interval, a rule is edited or a vehicle archived. Recomputing everything
  is simpler and harder to get wrong than updating reminders incrementally per event.
- **Cost:** a sync reads all active vehicles. Fine for one driver's garage; revisit if a
  sync ever shows up in a profile.

### Sync runs at the end of the list and detail `load()`

Those two screens already reload after every write and when the app opens, so syncing there
covers every change without each form knowing about reminders.

- **Cost:** a sync also runs when the user just navigates. It is idempotent and cheap.
- **Revisit when:** reads become reactive — then sync on data changes instead.

### Android: notifications are WorkManager jobs

Each reminder is a unique one-time `WorkRequest` named after its rule id, delayed until the
fire date, tagged so `pendingIdentifiers()` can list them. `ReminderWorker` posts the
notification when it runs.

- **Why:** WorkManager survives reboots and app updates and can list what is pending, which
  `ServiceReminderSync` needs to cancel stale reminders. `AlarmManager` can do neither.
- **Cost:** delivery is not to the minute — Android may batch the job while the phone dozes,
  so a 9:00 reminder can arrive a little later. Exact alarms need a special permission that a
  maintenance reminder does not justify.

### Android: asking for notification permission

`POST_NOTIFICATIONS` is a runtime permission. `NotificationPermission` asks through the
activity when `ServiceReminderSync` has something to schedule — the same moment iOS asks —
and answers "denied" when no activity is around.

### When a reminder fires

Reminders use the same status as the screen (`Maintenance.status`), so a notification never
calls something overdue that the app shows as due soon.

- **Ahead of time:** at 9:00 on the forecast due day.
- **Overdue** (calendar date passed, or the odometer reached the due km): at the next 9:00,
  daily for the first 7 days, then weekly, until the service is logged. "Overdue since" is
  the calendar due date, or the date of the first reading at or past the due km. Daily
  nagging forever teaches people to turn notifications off.
- **Pace estimate passed, not overdue:** no repeats. The notification already fired on the
  estimated day; repeating it daily for a car that sits in the garage would be noise. A rule
  that also has a months interval is still reminded at 9:00 on its calendar due day.
- **Km-only rule without a km/day pace** (one reading, or no movement): no date to schedule,
  so it is notified only once a logged reading reaches the due km. Readings only change when
  the user logs something, and every log triggers a sync, so that is when it fires.

When a rule is overdue both ways, "since" is the earlier of the two dates and the text says
"Overdue since" the calendar date. A rule with both intervals whose pace estimate has passed
still gets a reminder at 9:00 on its calendar due day.

Only one notification is pending per rule, and it is re-planned on every sync, so the daily
and weekly repeats happen only while the app keeps being opened.

Each notification names the vehicle and the service. Overdue by km says the due km and the
last reading, because the app only knows the last logged odometer, not the live one.
Permission to notify is asked only when there is something to schedule.

### `ServiceReminderSync` is the first use case

It coordinates three repositories and the scheduler and is triggered from two screens — the
trigger the "No use-case layer" decision set for revisiting. It lives in
`presentation/reminders` because it builds localized notification text; the scheduling rule
itself is in `domain/reminders/calculations/ReminderPlan.kt`.

### Service status: on track, due soon, overdue

Each reminder shows a status computed by `Maintenance.status(due, currentOdometer, now, zone)`,
counted from the last service of that kind (or the vehicle's creation).

- **Overdue** only when the calendar due date has passed or the odometer reached the due km.
  The distance-based date is an estimate from the km/day pace; a car parked for weeks must
  not turn red just because the estimate passed.
- **Due soon** when km left ≤ 15% of the interval, clamped to 300–1,000 km, or days left to
  the forecast date ≤ 15% of the period, clamped to 15–30 days. The floors keep short
  intervals (a 1,000 km chain) from warning one day ahead; the caps keep long ones (40,000 km
  spark plugs) from warning months ahead. Same rule for cars and motorcycles.
- A km-only rule on a vehicle without a km/day pace yet still shows, from km alone, with a
  hint to log another reading; it has no date until there is a second reading with distance.

## Presentation

### Dashboard on the vehicle screen

The detail screen leads with the odometer and km/month, a 2×2 grid of metrics, every
reminder with its status and progress, a km/L chart (two or more intervals) and a six-month
spending chart (months without records count as zero), then the history with an
All / Fuel / Services filter. Adding records moved to a bottom bar so it is one tap.

### New fuel-ups start with the last price

The price per liter is filled in from the latest fuel-up, with a footer saying so. Most
fill-ups happen at the same station, so this saves typing; the field stays editable.

### Category colors

Fuel is teal and services indigo everywhere (rows, charts, badges). Status uses green,
orange and red, so fuel deliberately avoids orange. The values are Apple's system colors, so
both apps look alike.

### Android: charts are drawn on a Canvas

There is no Swift Charts on Android, and a chart library would be the heaviest dependency of
the app for two small charts. `Charts.kt` draws the km/L line (with the dashed average) and
the stacked monthly bars with `Canvas` and `TextMeasurer`.

### Android: icons from Material Symbols

Each SF Symbol maps to the closest Material icon (`OilBarrel` for oil, `TireRepair` for tires,
`TwoWheeler` for motorcycles, …). Air and fuel filters, which share a symbol on iOS, get
different icons because Material has a filter icon.

## Tooling

### Four specialized AI agents instead of one

Development uses four Claude Code subagents — a car and motorcycle expert, an Android
specialist, a reviewer and an executor — with shared rules in `AGENTS.md`. Only the executor
edits code, and nothing is done until `make ci` passes. Rationale and workflow: [agents.md](agents.md).
