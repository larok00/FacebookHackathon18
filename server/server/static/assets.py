#!/usr/bin/env python
# -*- coding: utf-8 -*-

# This file is part of server.
# https://github.com/someuser/somepackage

# Licensed under the MIT license:
# http://www.opensource.org/licenses/MIT-license
# Copyright (c) 2018, Jacek Burys <jacekburys@gmail.com>

import os
import re

from flask import json
from flask.ext import assets

from webassets.updater import TimestampUpdater


STATIC_PATH = os.path.join(os.path.dirname(__file__))
VENDOR_PATH = os.path.join(STATIC_PATH, 'vendor')
PACKAGE_REGEX = re.compile(r'(?:(?:[├│└──┬]+)\s*)+\s([^#]+(?!.+extraneous))')

assets_env = assets.Environment()


def init_app(app):
    assets_env.app = app
    assets_env.init_app(app)

    assets_env.manifest = "file:%s" % (os.path.join(STATIC_PATH, '.webassets-manifest'))
    cache_path = app.config['WEBASSETS_CACHE_PATH']
    if not app.debug and not os.path.exists(cache_path):
        os.makedirs(cache_path)
    assets_env.cache = cache_path

    assets_env.load_path = STATIC_PATH
    assets_env.versions = 'hash:32'
    assets_env.auto_build = app.config['WEBASSETS_AUTO_BUILD']
    assets_env.url_expire = False

    # Tell flask-assets where to look for our coffeescript and scss files.
    assets_env.load_path = [
        os.path.join(os.path.dirname(__file__), 'scss'),
        os.path.join(os.path.dirname(__file__), 'coffee'),
        VENDOR_PATH
    ]

    bower_dependencies = read_bower_json()
    js_files = bower_dependencies.get('.js', [])

    js_out = 'js/js_app.%(version)s.js'
    if app.debug:
        js_out = 'js/js_app.js'

    coffee_files = get_coffee_files(app)
    if coffee_files:
        js_files.append(
            assets.Bundle(
                coffee_files,
                depends=('*.coffee'),
                # OTHER CONFIGS
                filters=['coffeescript'], output=js_out
            )
        )

    app.config['COMPASS_CONFIG'] = dict(
        encoding="utf-8",
        css_dir="css",
        fonts_dir="fonts",
        sass_dir="scss",
        images_dir="images",
        javascripts_dir="js",
        project_path=STATIC_PATH,
        relative_assets=True,
    )

    js_out = 'js/js_all.%(version)s.js'
    if app.debug:
        js_out = 'js/js_all.js'

    if js_files:
        js_all_bundle = assets.Bundle(
            *js_files,
            output=js_out
        )
    else:
        js_all_bundle = assets.Bundle()

    assets_env.register('js_all', js_all_bundle)

    scss_files = bower_dependencies.get('.scss', [])

    scss_bower_out = 'css/css_bower.%(version)s.css'
    if app.debug:
        scss_bower_out = 'css/css_bower.css'

    scss_bower_bundle = assets.Bundle(
        *scss_files,
        depends=('_*.scss'),
        filters=['compass'],
        output=scss_bower_out
    )

    css_out = 'css/css_all.%(version)s.css'
    if app.debug:
        css_out = 'css/css_all.css'

    css_all_bundle = assets.Bundle(
        'all.scss',
        depends=('_*.scss'),
        filters=['compass'],
        output=css_out
    )

    css_out = 'css/base.%(version)s.css'
    if app.debug:
        css_out = 'css/base.css'

    css_files = bower_dependencies.get('.css', [])
    css_base_bundle = assets.Bundle(
        *css_files,
        filters=['cssmin'],
        output=css_out
    )

    css_out = 'css/style.%(version)s.css'
    if app.debug:
        css_out = 'css/style.css'

    css_all_bundle = assets.Bundle(
        scss_bower_bundle,
        css_base_bundle,
        css_all_bundle,
        filters=['cssmin'],
        output=css_out
    )
    assets_env.register('css_all', css_all_bundle)



    if app.debug:
        assets_env.set_updater(TimestampUpdater())
        assets_env.cache = False
        assets_env.auto_build = True
        assets_env.debug = True


def read_bower_json():
    result = {}
    bower_dependencies = os.path.join(
        os.path.dirname(__file__), 'bower_dependencies.json'
    )

    if not os.path.exists(bower_dependencies):
        return result

    with open(bower_dependencies, 'r') as f:
        result = json.loads(f.read())
    for k, v in result.items():
        result[k] = [depFile.replace('server/static/vendor/', '') for depFile in v]
    return result


def get_coffee_files(app):
    coffee_files = []

    coffee_root = app.config['WEBASSETS_DIRECTORY']

    for filename in os.listdir(coffee_root):
        if os.path.splitext(filename)[1] == '.coffee':
            coffee_files.append(filename)

    return list(sorted(coffee_files))
