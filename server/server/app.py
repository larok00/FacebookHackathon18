#!/usr/bin/env python
# -*- coding: utf-8 -*-

# This file is part of server.
# https://github.com/someuser/somepackage

# Licensed under the MIT license:
# http://www.opensource.org/licenses/MIT-license
# Copyright (c) 2018, Jacek Burys <jacekburys@gmail.com>

import os
import os.path
import logging
import sys
import argparse
from urlparse import urlparse, urlunparse

from flask import Flask, request, redirect, current_app, url_for
from flask_debugtoolbar import DebugToolbarExtension

from werkzeug.utils import secure_filename
from server import config as config_module
from server.static import assets
from server import (
    handlers,


    admin,



)

blueprints = (
    handlers,
    assets,


    admin,


)


app = Flask(__name__)
    

def run_bower_list():
    bower_list = 'bower_list.js'
    try:
        os.system('node %s' % bower_list)
    except Exception:
        err = sys.exc_info()[1]
        print "Could not update bower list of assets (%s). Shutting down." % err
        sys.exit(1)


def create_app(config, debug=False):
    if config is None:
        config = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'config', 'local.conf'))

    app.debug = debug
    config_module.init_app(app, config)


    logging.basicConfig(level=logging.DEBUG)

    if app.debug:
        app.config['DEBUG_TB_PROFILER_ENABLED'] = True
        app.config['DEBUG_TB_INTERCEPT_REDIRECTS'] = False
        app.config['DEBUG_TB_PANELS'] = app.config.get('DEBUG_TB_PANELS', [
            'flask_debugtoolbar.panels.versions.VersionDebugPanel',
            'flask_debugtoolbar.panels.timer.TimerDebugPanel',
            'flask_debugtoolbar.panels.headers.HeaderDebugPanel',
            'flask_debugtoolbar.panels.request_vars.RequestVarsDebugPanel',
            'flask_debugtoolbar.panels.config_vars.ConfigVarsDebugPanel',
            'flask_debugtoolbar.panels.template.TemplateDebugPanel',
            'flask_debugtoolbar.panels.logger.LoggingPanel',
            'flask_debugtoolbar.panels.profiler.ProfilerDebugPanel',
        ])
        app.toolbar = DebugToolbarExtension(app)

    for blueprint in blueprints:
        blueprint.init_app(app)

    if app.debug:
        run_bower_list()

    return app


UPLOAD_FOLDER = '/home/dragos/envs/lsbaws/FacebookHackathon2018/server'
ALLOWED_EXTENSIONS = set(['txt', 'pdf', 'png', 'jpg', 'jpeg', 'gif'])
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER


def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/test', methods=['GET', 'POST'])
def upload_file():
    if request.method == 'POST':
        # check if the post request has the file part
        if 'file' not in request.files:
            flash('No file part')
            return redirect(request.url)
        file = request.files['file']
        # if user does not select file, browser also
        # submit a empty part without filename
        if file.filename == '':
            flash('No selected file')
            return redirect(request.url)
        if file and allowed_file(file.filename):
            filename = secure_filename(file.filename)
            file.save(os.path.join(app.config['UPLOAD_FOLDER'], filename))
            return redirect(url_for('upload_file',
                                   filename=filename))
    return '''<!doctype html><title>Upload new File</title><h1>Upload new File</h1><form method=post enctype=multipart/form-data><p><input type=file name=file><input type=submit value=Upload></form>'''

def main():
    args = parse_arguments()
    app = create_app(args.conf, debug=args.debug)
    app.run(debug=args.debug, host=args.bind, port=args.port, threaded=True)


def parse_arguments(args=None):
    if args is None:
        args = sys.argv[1:]

    parser = argparse.ArgumentParser()
    parser.add_argument('--port', '-p', type=int, default="3000", help="Port to start the server with.")
    parser.add_argument('--bind', '-b', default="0.0.0.0", help="IP to bind the server to.")
    parser.add_argument('--conf', '-c', default='server/config/local.conf', help="Path to configuration file.")
    parser.add_argument('--debug', '-d', action='store_true', default=False, help='Indicates whether to run in debug mode.')

    options = parser.parse_args(args)
    return options
