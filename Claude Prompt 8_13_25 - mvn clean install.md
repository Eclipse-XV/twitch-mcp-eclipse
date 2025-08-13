You are acting as my development assistant.  
We are working in a Git repository.  
Your task is to create a feature branch and update files according to the following plan:

Goal: Back out the current npm integration from the Java build process, restore a clean Maven-only build path, and isolate npm work into its own subfolder (`twitch-mcp-npm`), while keeping both systems separate for future re-integration. Also revise `readme-developer.md` accordingly.

Steps:

1. Create and switch to a new feature branch named:
   feature/remove-npm-integration

2. In `readme-developer.md`:
   - Compare it to the saved file `readme_old.md` (found in the same directory) and use it as a reference for restoring the Maven-first build instructions.
   - Update prerequisites:
     * Keep Java version as **Java 21** (not Java 11+).
     * Keep Apache and Quarkus requirements from `readme_old.md`.
     * Remove recommendations for AI CLI tools — developers can choose their own tools.
   - Remove all npm build instructions from the Java build process section.
   - Restore the old Maven build instructions:  
     ```
     mvn clean install
     ```
     Remove any instructions involving `maven clean package -Dskip...` or npm commands.
   - Ensure the npm instructions are separated into their own section for the `twitch-mcp-npm` subfolder, clearly stating that:
     * npm is **not** part of the Maven build.
     * npm work can be done independently without affecting the Maven build.
     * If needed, use `npm ci` inside `twitch-mcp-npm` when a `package-lock.json` is present.

3. Source changes:
   - Remove any integration points in the Maven `pom.xml` that automatically trigger npm commands.
   - Ensure Maven build works with `mvn clean install` without npm installed.
   - Keep the `twitch-mcp-npm` folder intact and untouched except for documenting its independent build process.

4. Validate:
   - Run `mvn clean install` to confirm the Java build succeeds without npm.
   - Run `npm ci` inside `twitch-mcp-npm` to confirm npm builds independently.

5. Commit changes:
   - Use commit message:  
     `feat: remove npm from Maven build and isolate npm workflow`
   - Push the feature branch.

Deliverables:
- Updated `readme-developer.md` reflecting the clean separation of Maven and npm workflows.
- Maven build working independently.
- npm build working in its own subfolder without affecting Maven.
