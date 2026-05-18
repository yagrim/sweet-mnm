.PHONY: help install test clean uninstall native native-install

PREFIX := $(HOME)/.local/bin
APPDIR := $(PREFIX)/sweet-mnm

# Detect OS and choose correct Gradle wrapper
ifeq ($(OS),Windows_NT)
	GRADLEW := gradlew.bat
else
	GRADLEW := ./gradlew
endif

help:
	@echo "Available targets:"
	@echo "  install  - Build and install distribution into ~/.local/bin"
	@echo "  test     - Run tests"
	@echo "  clean    - Clean build artifacts"

clean:
	@$(GRADLEW) clean
	@rm -rf build/

# setVersion will append the gitSha repeatedly.
# For now, we simply clean.
test: clean
	@$(GRADLEW) test

install: test
	@$(GRADLEW) installDist
	@mkdir -p $(PREFIX)
	@rm -rf $(APPDIR)
	@cp -r build/install/sweet $(APPDIR)
	@ln -sf $(APPDIR)/sweet $(PREFIX)/sweet-jvm

uninstall:
	@rm -rf $(APPDIR)
	@rm -f $(PREFIX)/sweet

native: test
	@$(GRADLEW) nativeCompile

native-install: install native
	@cp build/native/nativeCompile/sweet $(PREFIX)
