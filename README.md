[![Github CI](https://github.com/christophsturm/failgood-idea/workflows/Build/badge.svg)](https://github.com/christophsturm/failgood-idea/actions)

<!--
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
-->

# failgood-idea

<!--
## Template ToDo list
- [ ] Get familiar with the [template documentation][template].
- [ ] Verify the [pluginGroup](/gradle.properties), [plugin ID](/src/main/resources/META-INF/plugin.xml) and [sources package](/src/main/kotlin).
- [ ] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html).
- [ ] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate) for the first time.
- [ ] Set the Plugin ID in the above README badges.
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.
-->
<!-- Plugin description -->

        The Idea plugin for Failgood. Creates Run configurations for single tests or Sub-contexts from the Editor.

        Failgood works pretty well in idea without a plugin via the junit platform support.
        This plugin adds the option to create a run config that runs a single test or a subcontext.
<!-- Plugin description end -->
<!--
## Installation

- Using IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Failgood"</kbd> >
  <kbd>Install Plugin</kbd>

- Manually:

  Download the [latest release](https://github.com/christophsturm/failgood-idea/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
-->


### Contributing
run `./gradlew check`
