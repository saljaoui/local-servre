#!/bin/bash


# ╔══════════════════════════════════════════════════════════════════════════════╗
# ║                         LocalServer Run Script                               ║
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

# Extended color palette
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
    echo -e "${BRIGHT_BLUE}╔════════════════════════════════════════════╗${NC}"
    echo -e "${BRIGHT_BLUE}║${NC}        ${BOLD}${WHITE}LocalServer - Quick Run${NC}          ${BRIGHT_BLUE}║${NC}"
    echo -e "${BRIGHT_BLUE}╚════════════════════════════════════════════╝${NC}"
    echo ""
}


print_separator() {
    echo -e "${DIM}${CYAN}────────────────────────────────────────────────${NC}"
}


print_step() {
    local step=$1
    local total=$2
    local msg=$3
    echo -e "${BOLD}${SKY}[${WHITE}${step}${SKY}/${WHITE}${total}${SKY}]${NC} ${BRIGHT_CYAN}${msg}${NC}"
}


print_success() {
    echo -e "${GREEN}  [OK]${NC} ${WHITE}$1${NC}"
}


print_error() {
    echo -e "${RED}  [ERROR]${NC} ${WHITE}$1${NC}"
}


print_info() {
    echo -e "${CYAN}  [INFO]${NC} ${DIM}$1${NC}"
}


# ─────────────────────────────────────────────────────────────────────────────────
#                              Main Execution
# ─────────────────────────────────────────────────────────────────────────────────
clear
print_banner


# Override config file if provided
if [ ! -z "$1" ]; then
    CONFIG_FILE="$1"
    print_info "Using custom config: ${WHITE}$CONFIG_FILE${NC}"
    echo ""
fi


# ─────────────────────────── Step 1: Rebuild Project ─────────────────────────────
print_step "1" "4" "Rebuilding project"
print_separator

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
echo ""


# ─────────────────────────── Step 2: Verify Build ────────────────────────────────
print_step "2" "4" "Verifying build artifacts"
print_separator

if [ ! -d "$OUT_DIR" ]; then
    print_error "Build directory not found!"
    echo ""
    exit 1
fi

if [ ! -f "$OUT_DIR/Main.class" ]; then
    print_error "Main.class not found!"
    echo ""
    exit 1
fi

print_success "Build artifacts verified"
echo ""


# ─────────────────────────── Step 3: Check Config ────────────────────────────────
print_step "3" "4" "Loading configuration"
print_separator

if [ ! -f "$CONFIG_FILE" ]; then
    print_error "Config file not found: ${YELLOW}$CONFIG_FILE${NC}"
    echo ""
    exit 1
fi

print_success "Config file: ${CYAN}$CONFIG_FILE${NC}"
echo ""


# ─────────────────────────── Step 4: Prepare Dirs ────────────────────────────────
print_step "4" "4" "Preparing directories"
print_separator

mkdir -p www
mkdir -p www/uploads
mkdir -p cgi-bin
mkdir -p error_pages

print_success "Required directories ready"
echo ""


# ─────────────────────────── Launch Server ───────────────────────────────────────
echo -e "${BRIGHT_BLUE}════════════════════════════════════════════════${NC}"
echo -e "${BOLD}${BRIGHT_BLUE}  Starting LocalServer...${NC}"
echo -e "${BRIGHT_BLUE}════════════════════════════════════════════════${NC}"
echo -e "${DIM}${WHITE}Press ${BOLD}${YELLOW}Ctrl+C${NC}${DIM}${WHITE} to stop the server${NC}"
echo ""


java -cp "$OUT_DIR" Main "$CONFIG_FILE"


# ─────────────────────────── Shutdown Message ────────────────────────────────────
echo ""
echo -e "${BRIGHT_BLUE}════════════════════════════════════════════════${NC}"
echo -e "${BOLD}${CYAN}  Server Shutdown Complete${NC}"
echo -e "${BRIGHT_BLUE}════════════════════════════════════════════════${NC}"
echo ""
