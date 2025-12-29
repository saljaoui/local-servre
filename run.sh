#!/bin/bash

# ╔══════════════════════════════════════════════════════════════════════════════╗
# ║                         SonicServe Launch Script                             ║
# ╚══════════════════════════════════════════════════════════════════════════════╝

set -e  # Exit on error

# Navigate to project root
cd "$(dirname "$0")" || exit 1

# ─────────────────────────────────────────────────────────────────────────────────
# Colors
# ─────────────────────────────────────────────────────────────────────────────────

RESET='\033[0m'
BOLD='\033[1m'
DIM='\033[2m'
CYAN='\033[38;5;39m'
BRIGHT_CYAN='\033[38;5;51m'
GREEN='\033[38;5;46m'
YELLOW='\033[38;5;226m'
RED='\033[38;5;196m'
PURPLE='\033[38;5;147m'

# Sonic gradient colors (for logo)
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

log_step() {
    echo -e "${BOLD}${BRIGHT_CYAN}[$1]${RESET} ${PURPLE}$2${RESET}"
}

log_ok() {
    echo -e "  ${GREEN}✓${RESET} $1"
}

log_info() {
    echo -e "  ${CYAN}ℹ${RESET} ${DIM}$1${RESET}"
}

log_error() {
    echo -e "  ${RED}✗${RESET} $1"
}

divider() {
    echo -e "${DIM}${CYAN}  ─────────────────────────────────────────────────────────────────────────────${RESET}"
}

print_goodbye() {
    echo ""
    echo -e "${BOLD}${BRIGHT_CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
    echo -e "${BOLD}${BRIGHT_CYAN}                    Thank you for using SonicServe!${RESET}"
    echo -e "${DIM}${CYAN}                  Server stopped. See you next time! ⚡${RESET}"
    echo -e "${BOLD}${BRIGHT_CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
    echo ""
}

trap print_goodbye EXIT

# ─────────────────────────────────────────────────────────────────────────────────
# Main Build Process
# ─────────────────────────────────────────────────────────────────────────────────

clear
echo ""
echo -e "${BOLD}${BRIGHT_CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${BOLD}${BRIGHT_CYAN}                      SONICSERVE BUILD SYSTEM${RESET}"
echo -e "${BOLD}${BRIGHT_CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo ""

# Step 1: Dependencies
log_step "1/3" "Checking Dependencies"
divider

if ! command -v javac &> /dev/null; then
    log_error "Java compiler (javac) not found"
    log_info "Install JDK 8 or higher"
    exit 1
fi
log_ok "Java compiler $(javac -version 2>&1 | awk '{print $2}')"

if ! command -v java &> /dev/null; then
    log_error "Java runtime (java) not found"
    log_info "Install JRE 8 or higher"
    exit 1
fi
log_ok "Java runtime $(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')"

echo ""

# Step 2: Build
log_step "2/3" "Building Project"
divider

[ -d "build" ] && rm -rf build && log_info "Cleaned previous build"
mkdir -p build cgi-bin error_pages www uploads

log_info "Compiling source files..."
if javac -d build $(find src -name "*.java") 2>&1; then
    log_ok "Compiled $(find build -name "*.class" | wc -l) class files"
else
    log_error "Compilation failed"
    exit 1
fi

echo ""

# Step 3: Environment
log_step "3/3" "Verifying Environment"
divider

if [ ! -f "config/config.json" ]; then
    log_error "config/config.json not found"
    exit 1
fi
log_ok "Configuration file found"

for dir in cgi-bin error_pages www uploads; do
    [ ! -d "$dir" ] && mkdir -p "$dir"
done
log_ok "All directories ready"

# ─────────────────────────────────────────────────────────────────────────────────
# Launch
# ─────────────────────────────────────────────────────────────────────────────────

echo ""
echo -e "${BOLD}${BRIGHT_CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo ""
echo -e "${DIM}  Press ${BOLD}${YELLOW}Ctrl+C${RESET}${DIM} to stop the server${RESET}"
echo ""
print_sonic_logo
echo -e "${BOLD}${BRIGHT_CYAN}    Server is now running...${RESET}"
echo ""
echo -e "${BOLD}${BRIGHT_CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo ""

java -cp build Main "$@"