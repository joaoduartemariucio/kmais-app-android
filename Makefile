ifeq ($(OS),Windows_NT)
GRADLE := gradlew.bat --console=plain
PYTHON ?= python
else
GRADLE := ./gradlew --console=plain
PYTHON ?= python3
endif
COVERAGE_REPORT := app/build/reports/coverage/test/debug/report.xml
BASE ?= origin/main
MIN_COVERAGE ?= 80

.PHONY: ci lint format format-check build test coverage clean

ci: lint format-check build test coverage

lint:
	$(PYTHON) scripts/layer_rules.py
	$(GRADLE) :app:lintDebug

format:
	$(GRADLE) spotlessApply

format-check:
	$(GRADLE) spotlessCheck

build:
	$(GRADLE) :app:assembleDebug :app:assembleDebugUnitTest

test:
	$(GRADLE) :app:testDebugUnitTest :app:createDebugUnitTestCoverageReport

coverage:
	$(PYTHON) scripts/diff_coverage.py --report $(COVERAGE_REPORT) --base $(BASE) --min $(MIN_COVERAGE)

clean:
	$(GRADLE) clean
