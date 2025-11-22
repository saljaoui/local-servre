#!/bin/bash

# ╔══════════════════════════════════════════════════════════════════════════════╗
# ║                         LocalServer Launch Script                            ║
# ╚══════════════════════════════════════════════════════════════════════════════╝

# ─────────────────────────────────────────────────────────────────────────────────
#                              Change to Project Root
# ─────────────────────────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.." || exit 1

# ─────────────────────────────────────────────────────────────────────────────────
#                              Color Palette
# ─────────────────────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
BOLD='\033[1m'
DIM='\033[2m'
NC='\033[0m'

BRIGHT_BLUE='\033[1;34m'
BRIGHT_CYAN='\033[1;36m'
SKY='\033[38;5;117m'

# ─────────────────────────────────────────────────────────────────────────────────
#                              Configuration
# ─────────────────────────────────────────────────────────────────────────────────
OUT_DIR="out"
CONFIG_FILE="config/config.json"
BUILD_SCRIPT="scripts/build.sh"

# ─────────────────────────────────────────────────────────────────────────────────
#                              Helper Functions
# ─────────────────────────────────────────────────────────────────────────────────
print_banner() {
    echo ""
    echo -e "${BRIGHT_BLUE}    ██╗      ██████╗  ██████╗ █████╗ ██╗     ${NC}"
    echo -e "${BRIGHT_BLUE}    ██║     ██╔═══██╗██╔════╝██╔══██╗██║     ${NC}"
    echo -e "${BLUE}    ██║     ██║   ██║██║     ███████║██║     ${NC}"
    echo -e "${CYAN}    ██║     ██║   ██║██║     ██╔══██║██║     ${NC}"
    echo -e "${CYAN}    ███████╗╚██████╔╝╚██████╗██║  ██║███████╗${NC}"
    echo -e "${SKY}    ╚══════╝ ╚═════╝  ╚═════╝╚═╝  ╚═╝╚══════╝${NC}"
    echo ""
    echo -e "${BRIGHT_BLUE}    ███████╗███████╗██████╗ ██╗   ██╗███████╗██████╗ ${NC}"
    echo -e "${BRIGHT_BLUE}    ██╔════╝██╔════╝██╔══██╗██║   ██║██╔════╝██╔══██╗${NC}"
    echo -e "${BLUE}    ███████╗█████╗  ██████╔╝██║   ██║█████╗  ██████╔╝${NC}"
    echo -e "${CYAN}    ╚════██║██╔══╝  ██╔══██╗╚██╗ ██╔╝██╔══╝  ██╔══██╗${NC}"
    echo -e "${CYAN}    ███████║███████╗██║  ██║ ╚████╔╝ ███████╗██║  ██║${NC}"
    echo -e "${SKY}    ╚══════╝╚══════╝╚═╝  ╚═╝  ╚═══╝  ╚══════╝╚═╝  ╚═╝${NC}"
    echo ""
    echo -e "${DIM}${WHITE}                    High-Performance Java Server${NC}"
    echo ""
}

print_separator() {
    echo -e "${DIM}${CYAN}    ────────────────────────────────────────────────────────${NC}"
}

print_step() {
    local step=$1
    local total=$2
    local msg=$3
    echo -e "\n${BOLD}${SKY}    [${WHITE}${step}${SKY}/${WHITE}${total}${SKY}]${NC} ${BRIGHT_CYAN}${msg}${NC}"
    print_separator
}

print_success() {
    echo -e "${GREEN}       [OK]${NC} ${WHITE}$1${NC}"
}

print_error() {
    echo -e "${RED}       [ERROR]${NC} ${WHITE}$1${NC}"
}

print_info() {
    echo -e "${CYAN}       [INFO]${NC} ${DIM}$1${NC}"
}

print_warning() {
    echo -e "${YELLOW}       [WARNING]${NC} ${WHITE}$1${NC}"
}

# ─────────────────────────────────────────────────────────────────────────────────
#                              Main Execution
# ─────────────────────────────────────────────────────────────────────────────────
clear
print_banner

if [ ! -z "$1" ]; then
    CONFIG_FILE="$1"
    print_info "Using custom config: ${WHITE}$CONFIG_FILE${NC}"
fi

# ─────────────────────────── Step 1: Rebuild Project ─────────────────────────────
print_step "1" "4" "Rebuilding Project"

if [ -f "$BUILD_SCRIPT" ]; then
    print_info "Running ${YELLOW}scripts/build.sh${NC}..."
    echo ""
    bash "$BUILD_SCRIPT"
    if [ $? -eq 0 ]; then
        echo ""
        print_success "Build completed successfully"
    else
        echo ""
        print_error "Build failed!"
        exit 1
    fi
else
    print_error "Build script not found: ${YELLOW}$BUILD_SCRIPT${NC}"
    exit 1
fi

# ─────────────────────────── Step 2: Verify Build ────────────────────────────────
print_step "2" "4" "Verifying Build Artifacts"

if [ ! -d "$OUT_DIR" ] || [ ! -f "$OUT_DIR/Main.class" ]; then
    print_error "Build verification failed!"
    exit 1
fi

print_success "Build directory verified"
print_success "Main.class found"

# ─────────────────────────── Step 3: Check Config ────────────────────────────────
print_step "3" "4" "Loading Configuration"

if [ ! -f "$CONFIG_FILE" ]; then
    print_error "Config file not found: ${YELLOW}$CONFIG_FILE${NC}"
    exit 1
fi

print_success "Config loaded: ${CYAN}$CONFIG_FILE${NC}"

if command -v jq &> /dev/null && [ -f "$CONFIG_FILE" ]; then
    PORT=$(jq -r '.port // "8080"' "$CONFIG_FILE" 2>/dev/null)
    HOST=$(jq -r '.host // "localhost"' "$CONFIG_FILE" 2>/dev/null)
    if [ "$PORT" != "null" ] && [ "$HOST" != "null" ]; then
        print_info "Server will start on ${WHITE}http://${HOST}:${PORT}${NC}"
    fi
fi

# ─────────────────────────── Step 4: Prepare Dirs ────────────────────────────────
print_step "4" "4" "Preparing Environment"

dirs=("www" "www/uploads" "cgi-bin" "error_pages")
for dir in "${dirs[@]}"; do
    mkdir -p "$dir"
    print_success "Directory ready: ${DIM}${dir}/${NC}"
done

# ─────────────────────────── Launch Server ───────────────────────────────────────
echo ""
echo -e "${DIM}${CYAN}    ════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${BOLD}${BRIGHT_BLUE}    Server Starting...${NC}"
echo ""
echo -e "${DIM}${CYAN}    ════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${DIM}${WHITE}       ┌─────────────────────────────────────────────────┐${NC}"
echo -e "${DIM}${WHITE}       │${NC}   Press ${BOLD}${YELLOW}Ctrl+C${NC} to gracefully stop the server    ${DIM}${WHITE}│${NC}"
echo -e "${DIM}${WHITE}       └─────────────────────────────────────────────────┘${NC}"
echo ""
echo -e "${BRIGHT_BLUE}    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BOLD}${WHITE}                         SERVER LOG${NC}"
echo -e "${BRIGHT_BLUE}    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

java -cp "$OUT_DIR" Main "$CONFIG_FILE"

# ─────────────────────────── Shutdown Message ────────────────────────────────────
echo ""
echo -e "${BRIGHT_BLUE}    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BOLD}${CYAN}    Server Shutdown Complete${NC}"
echo -e "${BRIGHT_BLUE}    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
