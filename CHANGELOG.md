## [1.10.1]

### Changed
- updated test-resources
- Gradle-wrapper update
- dependency updates

## [1.10.0]

### Changed
- improved WSL support (WSL-distribution can now be selected in settings dialog)
- updated test-resources
- dependency updates

## [1.9.0]

### Added
- WSL support (see [README](https://github.com/4ch1m/intellij-ansible-lint?tab=readme-ov-file#requirements))

### Changed
- dependency updates

## [1.8.2]

### Changed
- improved process handling
- minor settings dialog adjustments
- updated test-resources
- Gradle-wrapper update
- other dependency updates

## [1.8.1]

### Fixed
- bug regarding path-handling in annotator

### Changed
- improved temp-file/-dir creation
- Gradle-wrapper update

## [1.8.0]

### Changed
- required IDE version >= `2024.2`
- major "Gradle IntelliJ Plugin" update (`1.17.4` to `2.0.1`)
- Gradle-wrapper update
- other dependency updates
- various minor code and asset improvements

## [1.7.1]

### Fixed
- `exclude_paths` now properly working on subdirectories

### Changed
- dependency updates
- minor code/asset improvements

## [1.7.0]

### Added
- evaluation/handling of `exclude_paths` in Ansible Lint configuration file
- new setting: lint files, even if they're within `exclude_paths` 

### Changed
- dependency updates
- Gradle-wrapper update

## [1.6.2]

### Fixed
- "Got it" tooltip creation... again ;-)

### Changed
- raised minimum platform target version (`2023.1`)
- minor code changes (resolved usage of deprecated methods)

## [1.6.1]

### Fixed
- "Got it" tooltip creation

## [1.6.0]

### Added
- "Got it" tooltip for status bar item

### Changed
- dependency updates
- Gradle-wrapper update

### Fixed
- more resilient parsing of `.ansible-lint-ignore` files

## [1.5.8]

### Changed
- updated test-resources
- dependency updates
- Gradle-wrapper update

## [1.5.7]

### Fixed
- the import of static resources (via "import_playbook", "import_tasks", etc.) doesn't show a "file not found"-error anymore

### Changed
- dependency updates
- Gradle-wrapper update

## [1.5.6]

### Fixed
- improved/fixed version parsing for "Test"-feature in settings dialog

### Changed
- dependency updates

## [1.5.5]

### Fixed
- proper visualization of severity levels (fixed in ansible-lint 6.21.0)

### Changed
- dependency updates
- Gradle-wrapper update

## [1.5.4]

### Changed
- updated SARIF schema
- dependency updates
- Gradle-wrapper update

## [1.5.3]

### Changed
- dependency updates
- updated test-resources
- Gradle-wrapper update
- several minor improvements

## [1.5.2]

### Fixed
- proper deletion of temporary files/directories

## [1.5.1]

### Fixed
- `ansible-lint` version detection in settings UI

## [1.5.0]

### Fixed
- incorrect "role not found" (`syntax-check[specific]`) errors

### Changed
- several minor improvements/additions
- dependency updates
- Gradle-wrapper update

## [1.4.1]

### Added
- rule tags are now being shown in annotation message (angle brackets)

### Changed
- several minor improvements
- dependency updates

## [1.4.0]

### Added
- enhanced output for "executable test" in settings dialog (min. version check)

### Changed
- major rewrite of internal parser: switch from CodeClimate-JSON to SARIF-JSON (see "[Output formats](https://ansible-lint.readthedocs.io/usage/#output-formats)")  
  **NOTE:** this requires `ansible-lint` **6.14.3** for best results (older versions should work; but provide less useful information in annotation messages) 
- improved formatting of annotation message(s)
- several other improvements/refactorings
- Gradle-wrapper update
- dependency updates

## [1.3.0]

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

## [1.2.1]

### Fixed
- use the system environment vars when running `ansible-lint`

## [1.2.0]

### Added
- new: quick actions
  - "Show detailed information online." opens the official (rule-specific) documentation in your browser
  - "Disable rule check using 'noqa'" automatically adds the _[noqa](https://ansible-lint.readthedocs.io/usage/#muting-warnings-to-avoid-false-positives)_ directive as a comment to the annotated line 
  - "Copy rule id to clipboard." does exactly that :)

### Fixed
- small fixes/improvements

## [1.1.0]

### Added
- donate link in settings

### Fixed
- small fixes/improvements

## [1.0.1]

### Changed
- minor improvements

## [1.0.0]

### Added
- initial release
