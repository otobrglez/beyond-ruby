# The Nix Shell support was added by Oto Brglez.
# If you have additional questions or need support, please reach out.
# - @otobrglez

{ pkgs ? import <nixpkgs> { } }:

with pkgs;

let
  sbt = pkgs.sbt.override { jre = jdk19_headless; };
  ruby = pkgs.ruby.withPackages (ps: with ps; [ nokogiri pry bundler ]);
  pyEnv = python3.withPackages (ps: with ps; [virtualenv pip ]);
in


mkShell {
  name = "beyond-ruby";
  buildInputs = [
    jdk19_headless
    pyEnv
    ruby
    sbt
  ];

  NIX_ENFORCE_PURITY = 0;
  NIX_SHELL_PRESERVE_PROMPT=1;
  
  shellHook = ''
    # set -x
    export JAVA_HOME="${jdk17_headless}"
    echo JAVA_HOME=$JAVA_HOME
    
    echo "Setup of Ruby"
    #mkdir -p .gem
    #export GEM_HOME=$PWD/.gem
    #export GEM_PATH=$GEM_HOME
    #export PATH=$GEM_HOME/bin:$PATH
    
    bundle config set --local path '.bundle' && bundle install

    echo "Setup of Python"
    export PIP_PREFIX=$(pwd)/_build/pip_packages
    export PYTHONPATH="$PIP_PREFIX/${pkgs.python3.sitePackages}:$PYTHONPATH"
    export PATH="$PIP_PREFIX/bin:$PATH"
    pip3 install -r requirements-dev.txt
  '';
}
