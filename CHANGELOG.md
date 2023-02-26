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
