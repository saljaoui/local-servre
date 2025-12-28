#!/bin/bash

# ╔══════════════════════════════════════════════════════════════════════════════╗
# ║                         SonicServe Launch Script                             ║
# ╚══════════════════════════════════════════════════════════════════════════════╝

# Change to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

# Colors (matching SonicLogger style)
RESET='\033[0m'
BOLD='\033[1m'
DIM='\033[2m'
GRAY='\033[38;5;242m'
CYAN='\033[38;5;39m'
BRIGHT_CYAN='\033[38;5;51m'
GREEN='\033[38;5;46m'
YELLOW='\033[38;5;226m'
RED='\033[38;5;196m'
PURPLE='\033[38;5;147m'

# Sonic gradient colors
SONIC_BLUE_1='\033[38;5;27m'
SONIC_BLUE_2='\033[38;5;33m'
SONIC_BLUE_3='\033[38;5;39m'
SONIC_BLUE_4='\033[38;5;45m'
SONIC_BLUE_5='\033[38;5;51m'
SONIC_BLUE_6='\033[38;5;87m'

# ─────────────────────────────────────────────────────────────────────────────────
# Functions
# ─────────────────────────────────────────────────────────────────────────────────

print_sonic_logo() {
    echo ""
    echo -e "${SONIC_BLUE_1}    ███████╗ ██████╗ ███╗   ██╗██╗ ██████╗ ${RESET}"
    echo -e "${SONIC_BLUE_2}    ██╔════╝██╔═══██╗████╗  ██║██║██╔════╝ ${RESET}"
    echo -e "${SONIC_BLUE_3}    ███████╗██║   ██║██╔██╗ ██║██║██║      ${RESET}"
    echo -e "${SONIC_BLUE_4}    ╚════██║██║   ██║██║╚██╗██║██║██║      ${RESET}"
    echo -e "${SONIC_BLUE_5}    ███████║╚██████╔╝██║ ╚████║██║╚██████╗ ${RESET}"
    echo -e "${SONIC_BLUE_6}    ╚══════╝ ╚═════╝ ╚═╝  ╚═══╝╚═╝ ╚═════╝ ${RESET}"
    echo ""
    echo -e "${SONIC_BLUE_1}    ███████╗███████╗██████╗ ██╗   ██╗███████╗██████╗ ${RESET}"
    echo -e "${SONIC_BLUE_2}    ██╔════╝██╔════╝██╔══██╗██║   ██║██╔════╝██╔══██╗${RESET}"
    echo -e "${SONIC_BLUE_3}    ███████╗█████╗  ██████╔╝██║   ██║█████╗  ██████╔╝${RESET}"
    echo -e "${SONIC_BLUE_4}    ╚════██║██╔══╝  ██╔══██╗╚██╗ ██╔╝██╔══╝  ██╔══██╗${RESET}"
    echo -e "${SONIC_BLUE_5}    ███████║███████╗██║  ██║ ╚████╔╝ ███████╗██║  ██║${RESET}"
    echo -e "${SONIC_BLUE_6}    ╚══════╝╚══════╝╚═╝  ╚═╝  ╚═══╝  ╚══════╝╚═╝  ╚═╝${RESET}"
    echo ""
}

log_info() {
    echo -e "${BOLD}${BRIGHT_CYAN}INFO${RESET}    ${GRAY}[shutdown]${RESET} : $1"
}

log_success() {
    echo -e "${BOLD}${GREEN}SUCCESS${RESET} ${GRAY}[shutdown]${RESET} : $1"
}

log_error() {
    echo -e "${BOLD}${RED}ERROR${RESET}   ${GRAY}[shutdown]${RESET} : $1"
}

# ─────────────────────────────────────────────────────────────────────────────────
# BUILD PROCESS
# ─────────────────────────────────────────────────────────────────────────────────

clear
echo ""
echo -e "${BOLD}${BRIGHT_CYAN}    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${BOLD}${BRIGHT_CYAN}                          SONICSERVE BUILD SYSTEM${RESET}"
echo -e "${BOLD}${BRIGHT_CYAN}    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo ""

echo -e "${BOLD}${BRIGHT_CYAN}[1/3]${RESET} ${PURPLE}Checking Dependencies${RESET}"
echo -e "${DIM}${CYAN}    ─────────────────────────────────────────────────────────────────────────────${RESET}"

# Check Java compiler
if ! command -v javac &> /dev/null; then
    echo -e "${RED}       [FAIL]${RESET} Java compiler (javac) not found!"
    echo -e "${CYAN}       [INFO]${RESET}  ${DIM}Please install JDK 8 or higher${RESET}"
    exit 1
fi
JAVAC_VERSION=$(javac -version 2>&1 | awk '{print $2}')
echo -e "${GREEN}       [OK]${RESET} Java compiler ready (version ${JAVAC_VERSION})"

# Check Java runtime
if ! command -v java &> /dev/null; then
    echo -e "${RED}       [FAIL]${RESET} Java runtime (java) not found!"
    echo -e "${CYAN}       [INFO]${RESET}  ${DIM}Please install JRE 8 or higher${RESET}"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
echo -e "${GREEN}       [OK]${RESET} Java runtime ready (version ${JAVA_VERSION})"

echo ""
echo -e "${BOLD}${BRIGHT_CYAN}[2/3]${RESET} ${PURPLE}Building Project${RESET}"
echo -e "${DIM}${CYAN}    ─────────────────────────────────────────────────────────────────────────────${RESET}"

# Clean build
if [ -d "build" ]; then
    rm -rf build
    echo -e "${CYAN}       [INFO]${RESET}  ${DIM}Cleaned previous build${RESET}"
fi

# Create directories
mkdir -p build cgi-bin error_pages www uploads
echo -e "${GREEN}       [OK]${RESET} Build directory created"

# Compile
echo -e "${CYAN}       [INFO]${RESET}  ${DIM}Compiling source files...${RESET}"
SOURCE_FILES=$(find src -name "*.java")
COMPILE_OUTPUT=$(javac -d build ${SOURCE_FILES} 2>&1)

if [ $? -ne 0 ]; then
    echo ""
    echo -e "${RED}       [FAIL]${RESET} Compilation failed!"
    echo -e "${DIM}${COMPILE_OUTPUT}${RESET}"
    exit 1
fi

COMPILED_COUNT=$(find build -name "*.class" | wc -l)
echo -e "${GREEN}       [OK]${RESET} Successfully compiled ${COMPILED_COUNT} class files"

echo ""
echo -e "${BOLD}${BRIGHT_CYAN}[3/3]${RESET} ${PURPLE}Verifying Environment${RESET}"
echo -e "${DIM}${CYAN}    ─────────────────────────────────────────────────────────────────────────────${RESET}"

# Check config
if [ ! -f "config/config.json" ]; then
    echo -e "${RED}       [FAIL]${RESET} Configuration file not found at config/config.json"
    echo -e "${CYAN}       [INFO]${RESET}  ${DIM}Please create a valid config.json file${RESET}"
    exit 1
fi
echo -e "${GREEN}       [OK]${RESET} Configuration file found"

# Check directories
DIRS=("cgi-bin" "error_pages" "www" "uploads")
for dir in "${DIRS[@]}"; do
    if [ ! -d "$dir" ]; then
        mkdir -p "$dir"
        echo -e "${CYAN}       [INFO]${RESET}  ${DIM}Created directory: ${dir}/${RESET}"
    fi
done
echo -e "${GREEN}       [OK]${RESET} All required directories ready"

# ─────────────────────────────────────────────────────────────────────────────────
# LAUNCH SERVER
# ─────────────────────────────────────────────────────────────────────────────────

echo ""
echo -e "${BOLD}${BRIGHT_CYAN}    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo ""
echo -e "${DIM}       Press ${BOLD}${YELLOW}Ctrl+C${RESET}${DIM} to stop the server${RESET}"
echo ""
print_sonic_logo
echo -e "${BOLD}${BRIGHT_CYAN}     Server is now running...${RESET}"
echo ""
echo -e "${BOLD}${BRIGHT_CYAN}    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo ""

# Launch server (ONLY ONCE!)
java -cp build Main "$@"
SERVER_EXIT_CODE=$?

# ─────────────────────────────────────────────────────────────────────────────────
# SHUTDOWN SEQUENCE
# ─────────────────────────────────────────────────────────────────────────────────

echo ""
echo -e "${BOLD}${BRIGHT_CYAN}    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"

if [ $SERVER_EXIT_CODE -eq 0 ]; then
    log_success "Server shutdown complete"
else
    log_error "Server terminated with exit code ${SERVER_EXIT_CODE}"
fi

echo -e "${BOLD}${BRIGHT_CYAN}    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${BOLD}${BRIGHT_CYAN}                           Thank you for using SonicServe!${RESET}"
echo ""
