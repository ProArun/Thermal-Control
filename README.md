# ThermalControl — AOSP Vendor Service for Raspberry Pi 5

A full-stack AOSP vendor component that exposes CPU temperature and fan control on a Raspberry Pi 5 running Android 15. Built entirely inside `vendor/myoem/` with no modifications to `frameworks/` or `device/`.

---

## Architecture

The project follows a 4-layer AOSP vendor stack:

```
┌─────────────────────────────────────────────────┐
│  ThermalMonitor App  (Kotlin + Jetpack Compose) │
│  apps/ThermalMonitor/                           │
└──────────────────┬──────────────────────────────┘
                   │  Java Binder (ServiceManager)
┌──────────────────▼──────────────────────────────┐
│  ThermalControlManager  (Java SDK library)      │
│  libs/thermalcontrol/                           │
└──────────────────┬──────────────────────────────┘
                   │  AIDL / Binder IPC (NDK)
┌──────────────────▼──────────────────────────────┐
│  thermalcontrold  (C++ native service)          │
│  services/thermalcontrol/                       │
└──────────────────┬──────────────────────────────┘
                   │  C++ interface call
┌──────────────────▼──────────────────────────────┐
│  libthermalcontrolhal  (C++ HAL library)        │
│  hal/thermalcontrol/                            │
│  reads/writes sysfs on RPi5 hardware            │
└─────────────────────────────────────────────────┘
```

---

## Layers

### HAL (`hal/thermalcontrol/`)
- `libthermalcontrolhal` — `cc_library_shared` built with Soong
- Reads CPU temperature from `/sys/class/thermal/thermal_zone0/temp`
- Controls fan PWM via `/sys/class/hwmon/hwmonN/pwm1` and `pwm1_enable`
- Dynamically discovers the hwmon sysfs index at init (not hardcoded)
- Supports read/write/auto mode; degrades gracefully when hardware is absent

### Service (`services/thermalcontrol/`)
- `thermalcontrold` — `cc_binary` registered as a `@VintfStability` AIDL service
- AIDL interface: `IThermalControlService` (NDK backend, `libbinder_ndk`)
- Launched via `init.rc`; managed by Android's `init` process
- SELinux policy (`thermalcontrold.te`, `service_contexts`, `file_contexts`)

### Manager Library (`libs/thermalcontrol/`)
- `thermalcontrol-manager` — `java_sdk_library`
- Wraps the AIDL stub with safe defaults and `RemoteException` handling
- Exposes a clean Java API: `getCpuTemperatureCelsius()`, `setFanSpeed(int)`, etc.

### App (`apps/ThermalMonitor/`)
- Jetpack Compose UI with real-time polling (2 s interval via `StateFlow`)
- Displays CPU temperature with color-coded heat category (Cool / Warm / Hot / Critical)
- Fan control: On / Off / Auto mode / speed slider (0–100%)
- MVI pattern: `UiState`, `ThermalViewModel`, stateless `ThermalScreen` composable

---

## Build Targets

```bash
lunch myoem_rpi5-trunk_staging-userdebug

m libthermalcontrolhal          # HAL shared library
m thermalcontrold               # native service binary
m thermalcontrol-manager        # Java SDK library
m ThermalMonitor                # Compose app
m VtsThermalControlHalTest      # HAL unit tests
m VtsThermalControlServiceTest  # VTS binder tests
```

---

## Testing

| Test suite | Type | Target |
|---|---|---|
| `VtsThermalControlHalTest` | GTest (native) | HAL sysfs read/write, boundary checks, graceful degradation |
| `VtsThermalControlServiceTest` | VTS / GTest | VINTF compliance, binder IPC, error codes, concurrency |
| `ThermalControlManagerTest` | JUnit (Java) | Manager null-binder safety, range validation |
| `ThermalScreenTest` | Compose instrumented | UI state rendering, button interactions, state transitions |

---

## Key Technical Details

- **AIDL backend**: NDK (`libbinder_ndk`) — required for vendor processes in AOSP 15
- **VINTF stability**: `@VintfStability` annotation with frozen API snapshot (`aidl_api/`)
- **SELinux**: custom `thermalcontrold` domain; hwmon files use generic `sysfs` label (not `sysfs_hwmon`, which does not exist in AOSP 15 base policy)
- **hwmon discovery**: scanned dynamically at startup since the hwmon index is not stable across kernel versions or boot order
- **RC file**: `chown root system` + `chmod 0664` on hwmon sysfs nodes at `on boot` — kernel creates them as `root:root 0644`, blocking writes from the `system` user

---

## Environment

| Item | Value |
|---|---|
| Device | Raspberry Pi 5 |
| AOSP branch | `android-15.0.0_r14` |
| Build system | Soong (Android.bp) |
| Languages | C++17, AIDL, Java, Kotlin |
