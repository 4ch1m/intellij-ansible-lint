# Changelog

## Unreleased

## 1.12.1 - 2026-07-16

### Changed

- updated test-tools/-resources
- Gradle-wrapper update
- dependency updates

## 1.12.0 - 2026-04-25

### Changed

- required IDE version >= `2025.3`
- replaced API calls marked as `deprecated`
- updated test-tools/-resources
- Gradle-wrapper update
- dependency updates

## 1.11.5 - 2026-02-01

### Changed

- updated test-tools/-resources
- Gradle-wrapper update
- dependency updates

## 1.11.4 - 2025-11-23

### Changed

- minor improvements in settings dialog
- updated test-tools/-resources
- Gradle-wrapper update
- dependency updates

## 1.11.3 - 2025-10-14

### Changed

- updated test-tools/-resources
- Gradle-wrapper update
- dependency updates

## 1.11.2 - 2025-08-18

### Changed

- new plugin icon
- dependency updates

## 1.11.1 - 2025-08-08

### Changed

- updated test-resources
- Gradle-wrapper update
- dependency updates

## 1.11.0 - 2025-04-23

### Changed

- required IDE version >= `2025.1`
- updated test-resources
- Gradle-wrapper update
- dependency updates

## 1.10.1 - 2025-02-16

### Changed

- updated test-resources
- Gradle-wrapper update
- dependency updates

## 1.10.0 - 2024-12-15

### Changed

- improved WSL support (WSL-distribution can now be selected in settings dialog)
- updated test-resources
- dependency updates

## 1.9.0 - 2024-10-22

### Added

- WSL support (see [README](https://github.com/4ch1m/intellij-ansible-lint?tab=readme-ov-file#requirements))

### Changed

- dependency updates

## 1.8.2 - 2024-09-25

### Changed

- improved process handling
- minor settings dialog adjustments
- updated test-resources
- Gradle-wrapper update
- other dependency updates

## 1.8.1 - 2024-08-28

### Fixed

- bug regarding path-handling in annotator

### Changed

- improved temp-file/-dir creation
- Gradle-wrapper update

## 1.8.0 - 2024-08-12

### Changed

- required IDE version >= `2024.2`
- major "Gradle IntelliJ Plugin" update (`1.17.4` to `2.0.1`)
- Gradle-wrapper update
- other dependency updates
- various minor code and asset improvements

## 1.7.1 - 2024-08-05

### Fixed

- `exclude_paths` now properly working on subdirectories

### Changed

- dependency updates
- minor code/asset improvements

## 1.7.0 - 2024-07-27

### Added

- evaluation/handling of `exclude_paths` in Ansible Lint configuration file
- new setting: lint files, even if they're within `exclude_paths` 

### Changed

- dependency updates
- Gradle-wrapper update

## 1.6.2 - 2024-03-29

### Fixed

- "Got it" tooltip creation... again ;-)

### Changed

- raised minimum platform target version (`2023.1`)
- minor code changes (resolved usage of deprecated methods)

## 1.6.1 - 2024-03-29

### Fixed

- "Got it" tooltip creation

## 1.6.0 - 2024-03-27

### Added

- "Got it" tooltip for status bar item

### Changed

- dependency updates
- Gradle-wrapper update

### Fixed

- more resilient parsing of `.ansible-lint-ignore` files

## 1.5.8 - 2024-02-24

### Changed

- updated test-resources
- dependency updates
- Gradle-wrapper update

## 1.5.7 - 2023-12-02

### Fixed

- the import of static resources (via "import_playbook", "import_tasks", etc.) doesn't show a "file not found"-error anymore

### Changed

- dependency updates
- Gradle-wrapper update

## 1.5.6 - 2023-11-09

### Fixed

- improved/fixed version parsing for "Test"-feature in settings dialog

### Changed

- dependency updates

## 1.5.5 - 2023-10-18

### Fixed

- proper visualization of severity levels (fixed in ansible-lint 6.21.0)

### Changed

- dependency updates
- Gradle-wrapper update

## 1.5.4 - 2023-08-20

### Changed

- updated SARIF schema
- dependency updates
- Gradle-wrapper update

## 1.5.3 - 2023-07-03

### Changed

- dependency updates
- updated test-resources
- Gradle-wrapper update
- several minor improvements

## 1.5.2 - 2023-05-26

### Fixed

- proper deletion of temporary files/directories

## 1.5.1 - 2023-05-19

### Fixed

- `ansible-lint` version detection in settings UI

## 1.5.0 - 2023-05-17

### Fixed

- incorrect "role not found" (`syntax-check[specific]`) errors

### Changed

- several minor improvements/additions
- dependency updates
- Gradle-wrapper update

## 1.4.1 - 2023-04-07

### Added

- rule tags are now being shown in annotation message (angle brackets)

### Changed

- several minor improvements
- dependency updates

## 1.4.0 - 2023-03-24

### Added

- enhanced output for "executable test" in settings dialog (min. version check)

### Changed

- major rewrite of internal parser: switch from CodeClimate-JSON to SARIF-JSON (see "[Output formats](https://ansible-lint.readthedocs.io/usage/#output-formats)")  
  **NOTE:** this requires `ansible-lint` **6.14.3** for best results (older versions should work; but provide less useful information in annotation messages) 
- improved formatting of annotation message(s)
- several other improvements/refactorings
- Gradle-wrapper update
- dependency updates

## 1.3.0 - 2023-02-27

### Added

- new: [ignore-file](https://ansible-lint.readthedocs.io/configuring/#ignoring-rules-for-entire-files) integration
  - new quick action ("Add rule to ignore-file.")
  - general plugin setting to control visualization of ignored rules
- new: quick action "Add rule id to 'skip_list' in config file."

### Changed

- Gradle-wrapper update
- dependency updates

### Fixed

- various fixes, improvements, and refactorings

## 1.2.1 - 2023-02-07

### Fixed

- use the system environment vars when running `ansible-lint`

## 1.2.0 - 2023-02-04

### Added

- new: quick actions
  - "Show detailed information online." opens the official (rule-specific) documentation in your browser
  - "Disable rule check using 'noqa'" automatically adds the _[noqa](https://ansible-lint.readthedocs.io/usage/#muting-warnings-to-avoid-false-positives)_ directive as a comment to the annotated line 
  - "Copy rule id to clipboard." does exactly that :)

### Fixed

- small fixes/improvements

## 1.1.0 - 2023-01-27

### Added

- donate link in settings

### Fixed

- small fixes/improvements

## 1.0.1 - 2023-01-22

### Changed

- minor improvements

## 1.0.0 - 2023-01-22

### Added

- initial release
