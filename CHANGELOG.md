# Changelog

All notable changes to the Twitch MCP Server project will be documented in this file.

## [1.1.3] - 2025-08-13

### Added - NPM Wrapper Restoration Merge

This release merges the stable Java codebase with a restored NPM wrapper system for improved developer experience and easy distribution.

#### 🔧 NPM Build System
- **npm scripts**: Added `build:jar`, `start`, and `test:twitch` commands
- **JavaScript wrapper**: Smart JAR detection and building with safety checks
- **Developer tools**: Automated build detection, git safety, and environment validation

#### 📚 Enhanced Documentation  
- **Split README structure**: User-focused `README.md` and developer-focused `README-developers.md`
- **AI CLI recommendations**: Added guidance for Gemini CLI (primary), Qwen Coder CLI (secondary), and Claude Code (power users)
- **Claude integration**: Comprehensive `CLAUDE.md` with MCP tools and usage examples

#### 🔒 Safety & Security
- **Enhanced .gitignore**: Prevents JAR files and credentials from being committed
- **Docker ignore**: Updated `.dockerignore` to exclude build artifacts and secrets
- **Git safety checks**: NPM wrapper warns if JAR files are staged for commit

#### 🚀 Developer Experience
- **One-command setup**: `npm ci && npm run start` for immediate local development
- **Smart JAR building**: Only rebuilds when source files change
- **Environment validation**: Comprehensive configuration checks and troubleshooting

#### 🎯 Architecture Improvements
- **Layered structure**: Clear separation between MCP, service, integration, and API layers
- **Configuration flexibility**: Support for both environment variables and config files
- **Cross-platform compatibility**: Works on Windows, macOS, and Linux

### Technical Details

**Build System**: Maven-based with NPM wrapper for distribution  
**Runtime**: Java 11+ with Node.js 14+ for tooling  
**Distribution**: NPM package with automatic JAR bundling  
**Safety**: JAR files never committed to git, dist/ directory ignored  

### Migration Notes

This release maintains full backward compatibility with existing configurations while adding modern NPM-based tooling for easier development and deployment.

**For End Users**: Use `npx twitch-mcp-server@latest` - no changes needed  
**For Developers**: New NPM scripts provide better development workflow  
**For Contributors**: See `README-developers.md` for updated contribution guidelines  

### Known Limitations

- **Automated testing**: Currently manual testing only (automated tests planned for future release)
- **CI/CD**: Build pipeline needs to be established in follow-up work
- **Documentation**: Some advanced configuration scenarios need additional examples

---

## Future Releases

### Planned for v1.2.0
- **Automated testing suite**: Unit tests for MCP tools, integration tests for Twitch API
- **CI/CD pipeline**: Automated building, testing, and NPM publishing  
- **Advanced moderation**: Enhanced chat analysis and moderation capabilities
- **Performance optimization**: Improved memory usage and connection handling

---

*This changelog follows the [Keep a Changelog](https://keepachangelog.com/) format.*