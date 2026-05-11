.PHONY: help install test clean

PREFIX := $(HOME)/.local/bin
APPDIR := $(PREFIX)/sweet-mnm

help:
	@echo "Available targets:"
	@echo "  install  - Build and install distribution into ~/.local/bin"
	@echo "  test     - Run tests"
	@echo "  clean    - Clean build artifacts"

clean:
	@./gradlew clean
	@rm -rf build/

# setVersion will append the gitSha repeatedly.
# For now, we simply clean.
test: clean
	@./gradlew test

install: test
	@./gradlew installDist
	@mkdir -p $(PREFIX)
	@rm -rf $(APPDIR)
	@cp -r build/install/sweet $(APPDIR)
	@ln -sf $(APPDIR)/sweet $(PREFIX)/sweet-jvm

uninstall:
	@rm -rf $(APPDIR)
	@rm $(PREFIX)/sweet

native: test
	@./gradlew nativeCompile

native-install: install native
	@cp build/native/nativeCompile/sweet $(PREFIX)
