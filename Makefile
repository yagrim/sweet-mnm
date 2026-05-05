.PHONY: help install test clean

PREFIX := $(HOME)/.local/bin
APPDIR := $(PREFIX)/sweet-mnm

help:
	@echo "Available targets:"
	@echo "  install  - Build and install distribution into ~/.local/bin"
	@echo "  test     - Run tests"
	@echo "  clean    - Clean build artifacts"

install: clean test
	@./gradlew installDist
	@mkdir -p $(PREFIX)
	@rm -rf $(APPDIR)
	@cp -r build/install/sweet $(APPDIR)
	@ln -sf $(APPDIR)/sweet $(PREFIX)/sweet

test:
	@./gradlew test

clean:
	@./gradlew clean
	@rm -rf build/