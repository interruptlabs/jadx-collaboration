## JADX Collaboration

A plugin to enable users to collaborate on a JADX-GUI project.

## Installation

This plugin currently requires a development version of JADX. Either [build from source](https://github.com/skylot/jadx?tab=readme-ov-file#build-from-source) or use the following steps.

- Navigate [here](https://github.com/skylot/jadx/actions/workflows/build-artifacts.yml).
- Select the latest run.
- Download a build artefact (seems to require sign-in).

In JADX-GUI:

- Navigate to `File > Preferences > Plugins`.
- Click `Install plugin`.
- Enter `github:Oshawk:jadx-collaboration` as the `Location id`.
- Click `Install` and `Save`.
- If there is a project open, use `File > Reload files`.

## Setup

Start by creating a remote git repository that you and your collaborators will have access to. *If you already have a remote repository for JADX collaboration, this step can be skipped.*

Clone the remote repository to create a local version. If the repository has no commits, create one and push it (`git commit --allow-empty -m 'Initial commit' && git push`). *If you already have a local repository for JADX collaboration, this step can be skipped.*

Download a pair of `pull` and `push` scripts from [here](/tree/main/scripts) (`.ps1` for Windows and `.sh` for Mac or Linux). If you are using Mac or Linux, ensure the scripts are executable (`chmod +x pull.sh push.sh`). *If you already have pre-pull and post-push scripts downloaded, this step can be skipped.*

Now, in JADX-GUI:

- Open the file you want to collaborate on.
- Save the JADX project (the location doesn't matter).
- Navigate to `File > Preferences > Plugins > JADX Collaboration`.
- In `Path to the repository file` enter the absolute path to a file in the root of your local repository (it does not have to exist). This file will be used to share changes between collaborators.
- In `Path to the pre-pull script` enter the absolute path to the `pull` script you downloaded earlier.
- In `Path to the post-push script` enter the absolute path to the `push` script you downloaded earlier.
- Click `Save` and use `File > Reload files` to apply the changes.

It is important that all collaborators:

- Work on the exact same Java file (distribution of Java files is not handled by the plugin).
- Set `Path to the repository file` to the same file (but in their own local repository).

## Usage

To pull changes from the remote repository, use `Plugins > Pull` or `CTRL + \`.

To push changes to the remote repository (will also pull), use `Plugins > Push` or `CTRL + SHIFT + \`.

## Limitations

- Both pull and push operations reload files, which could be an issue for large projects. This could probably be solved by selectively sending events, but that would be far more complex that the current solution.
- Currently only renames are shared. It would probably be possible to add comment sharing in the future.

## Advanced Setup

TODO
