#!/bin/bash


# ╔══════════════════════════════════════════════════════════════════════════════╗
# ║                         SonicServe Launch Script                             ║
# ╚══════════════════════════════════════════════════════════════════════════════╝


# Change to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.." || exit 1


# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
BOLD='\033[1m'
DIM='\033[2m'
NC='\033[0m'


# ─────────────────────────────────────────────────────────────────────────────────
# ASCII Art Functions
# ─────────────────────────────────────────────────────────────────────────────────


print_sonic_logo() {
    echo ""
    echo -e "\033[1;38;5;27m    ███████╗ ██████╗ ███╗   ██╗██╗ ██████╗ \033[0m"
    echo -e "\033[1;38;5;33m    ██╔════╝██╔═══██╗████╗  ██║██║██╔════╝ \033[0m"
    echo -e "\033[1;38;5;39m    ███████╗██║   ██║██╔██╗ ██║██║██║      \033[0m"
    echo -e "\033[1;38;5;45m    ╚════██║██║   ██║██║╚██╗██║██║██║      \033[0m"
    echo -e "\033[1;38;5;51m    ███████║╚██████╔╝██║ ╚████║██║╚██████╗ \033[0m"
    echo -e "\033[1;38;5;87m    ╚══════╝ ╚═════╝ ╚═╝  ╚═══╝╚═╝ ╚═════╝ \033[0m"
    echo ""
    echo -e "\033[1;38;5;27m    ███████╗███████╗██████╗ ██╗   ██╗███████╗██████╗ \033[0m"
    echo -e "\033[1;38;5;33m    ██╔════╝██╔════╝██╔══██╗██║   ██║██╔════╝██╔══██╗\033[0m"
    echo -e "\033[1;38;5;39m    ███████╗█████╗  ██████╔╝██║   ██║█████╗  ██████╔╝\033[0m"
    echo -e "\033[1;38;5;45m    ╚════██║██╔══╝  ██╔══██╗╚██╗ ██╔╝██╔══╝  ██╔══██╗\033[0m"
    echo -e "\033[1;38;5;51m    ███████║███████╗██║  ██║ ╚████╔╝ ███████╗██║  ██║\033[0m"
    echo -e "\033[1;38;5;87m    ╚══════╝╚══════╝╚═╝  ╚═╝  ╚═══╝  ╚══════╝╚═╝  ╚═╝\033[0m"
    echo ""
}


print_header() {
    echo ""
    echo -e "\033[1;38;5;39m    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BOLD}\033[1;38;5;51m                          SONICSERVE BUILD SYSTEM${NC}"
    echo -e "\033[1;38;5;39m    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}


print_separator() {
    echo -e "${DIM}${CYAN}    ─────────────────────────────────────────────────────────────────────────────${NC}"
}


print_heavy_separator() {
    echo -e "\033[1;38;5;39m    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}


print_step() {
    local step=$1
    local total=$2
    local msg=$3
    echo ""
    echo -e "${BOLD}\033[1;38;5;51m    [${WHITE}${step}\033[1;38;5;51m/${WHITE}${total}\033[1;38;5;51m]${NC} \033[1;38;5;45m${msg}${NC}"
    print_separator
}


print_success() {
    echo -e "${GREEN}       ✅${NC} ${WHITE}$1${NC}"
}


print_error() {
    echo -e "${RED}       ❌${NC} ${WHITE}$1${NC}"
}


print_info() {
    echo -e "${CYAN}       ℹ${NC}  ${DIM}$1${NC}"
}


# ─────────────────────────────────────────────────────────────────────────────────
# MAIN BUILD PROCESS
# ─────────────────────────────────────────────────────────────────────────────────


clear
print_header


# Step 1: Check Dependencies
print_step "1" "3" "Checking Dependencies"


if ! command -v javac &> /dev/null; then
    print_error "Java compiler (javac) not found!"
    print_info "Please install JDK 8 or higher"
    exit 1
fi
JAVAC_VERSION=$(javac -version 2>&1 | awk '{print $2}')
print_success "Java compiler ready (version ${JAVAC_VERSION})"


if ! command -v java &> /dev/null; then
    print_error "Java runtime (java) not found!"
    print_info "Please install JRE 8 or higher"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
print_success "Java runtime ready (version ${JAVA_VERSION})"


# Step 2: Clean and Build
print_step "2" "3" "Building Project"


if [ -d "build" ]; then
    rm -rf build
    print_info "Cleaned previous build"
fi


mkdir -p build
print_success "Build directory created"


print_info "Compiling source files..."
COMPILE_OUTPUT=$(javac -d build \
    src/config/*.java \
    src/utils/*.java \
    src/request/*.java \
    src/response/*.java \
    src/cgi/*.java \
    src/server/*.java \
    src/Main.java 2>&1)


if [ $? -ne 0 ]; then
    echo ""
    print_error "Compilation failed!"
    echo -e "${DIM}${WHITE}${COMPILE_OUTPUT}${NC}"
    exit 1
fi


COMPILED_COUNT=$(find build -name "*.class" | wc -l)
print_success "Successfully compiled ${COMPILED_COUNT} class files"


# Step 3: Verify Environment
print_step "3" "3" "Verifying Environment"


if [ ! -f "config/config.json" ]; then
    print_error "Configuration file not found at config/config.json"
    print_info "Please create a valid config.json file"
    exit 1
fi
print_success "Configuration file found"


DIRS=("cgi-bin" "error_pages" "www" "uploads")
for dir in "${DIRS[@]}"; do
    if [ ! -d "$dir" ]; then
        mkdir -p "$dir"
        print_info "Created directory: ${dir}/"
    fi
done
print_success "All required directories ready"


# ─────────────────────────────────────────────────────────────────────────────────
# LAUNCH SERVER
# ─────────────────────────────────────────────────────────────────────────────────


echo ""
print_heavy_separator
echo ""
echo -e "${DIM}${WHITE}       ┌──────────────────────────────────────────────────────────────────┐${NC}"
echo -e "${DIM}${WHITE}       │                ${NC}  Press ${BOLD}${YELLOW}Ctrl+C${NC}${DIM} to stop the server${DIM}${WHITE}                 │${NC}"
echo -e "${DIM}${WHITE}       └──────────────────────────────────────────────────────────────────┘${NC}"
echo ""
print_sonic_logo
echo -e "${BOLD}\033[1;38;5;45m     Server is now running...${NC}"
echo ""
print_heavy_separator
echo ""


java -cp build Main
SERVER_EXIT_CODE=$?


# ─────────────────────────────────────────────────────────────────────────────────
# SHUTDOWN SEQUENCE
# ─────────────────────────────────────────────────────────────────────────────────


echo ""
print_heavy_separator


if [ $SERVER_EXIT_CODE -eq 0 ]; then
    echo -e "${BOLD}\033[1;38;5;51m    ✓ Server Shutdown Complete${NC}"
else
    echo -e "${BOLD}${RED}    ✗ Server Terminated (Exit Code: ${SERVER_EXIT_CODE})${NC}"
fi


print_heavy_separator
echo -e "${DIM}${CYAN}                           Thank you for using SonicServe!${NC}"
echo ""
