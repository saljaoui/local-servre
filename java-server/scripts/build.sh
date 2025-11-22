#!/bin/bash


# ╔══════════════════════════════════════════════════════════════════════════════╗
# ║                         LocalServer Build Script                             ║
# ╚══════════════════════════════════════════════════════════════════════════════╝


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
SRC_DIR="src"


# ─────────────────────────────────────────────────────────────────────────────────
#                              Helper Functions
# ─────────────────────────────────────────────────────────────────────────────────

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


# ─────────────────────────── Step 1: Clean Build ─────────────────────────────────
print_step "1" "3" "Cleaning Build Directory"

if [ -d "$OUT_DIR" ]; then
    rm -rf "$OUT_DIR"
    print_success "Removed old build directory"
fi

mkdir -p "$OUT_DIR"
print_success "Created fresh build directory: ${CYAN}$OUT_DIR/${NC}"


# ─────────────────────────── Step 2: Find Sources ────────────────────────────────
print_step "2" "3" "Discovering Java Source Files"

if [ ! -d "$SRC_DIR" ]; then
    print_error "Source directory not found: ${YELLOW}$SRC_DIR/${NC}"
    echo ""
    exit 1
fi

# Find all .java files and count them
JAVA_FILES=$(find "$SRC_DIR" -type f -name "*.java")
FILE_COUNT=$(echo "$JAVA_FILES" | wc -l)

if [ -z "$JAVA_FILES" ] || [ "$FILE_COUNT" -eq 0 ]; then
    print_error "No .java files found in ${YELLOW}$SRC_DIR/${NC}"
    echo ""
    exit 1
fi

print_success "Found ${WHITE}$FILE_COUNT${NC} Java source file(s)"


# ─────────────────────────── Step 3: Compile ──────────────────────────────────────
print_step "3" "3" "Compiling Java Sources"

print_info "Running ${YELLOW}javac${NC}..."
echo ""

# Compile all Java files at once
find "$SRC_DIR" -type f -name "*.java" -exec javac -d "$OUT_DIR" -cp "$SRC_DIR" '{}' +

if [ $? -ne 0 ]; then
    echo ""
    print_error "Compilation failed!"
    print_info "Fix the errors above and try again"
    echo ""
    exit 1
fi

# Verify Main.class exists
if [ ! -f "$OUT_DIR/Main.class" ]; then
    echo ""
    print_warning "Main.class not found!"
    print_info "Ensure you have a Main.java with a main() method"
    echo ""
    exit 1
fi

print_success "Compilation completed successfully"
print_success "Main.class generated"


# ─────────────────────────── Build Complete ───────────────────────────────────────
echo ""
echo -e "${DIM}${CYAN}    ════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${BOLD}${GREEN}    ✓ Build Complete!${NC}"
echo ""
echo -e "${DIM}${CYAN}    ════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${CYAN}       [INFO]${NC} ${DIM}Run ${WHITE}./launch.sh${DIM} to start the server${NC}"
echo ""
