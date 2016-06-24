/*!
 * OpenCPS PKI; version: 1.0.0
 * https://github.com/VietOpenCPS/pki
 * Copyright (c) 2016 OpenCPS Community;
 * Licensed under the AGPL V3+.
 * https://www.gnu.org/licenses/agpl-3.0.html
 */
(function($) {
"use strict";

function hex2Array(hex) {
    if(typeof hex == 'string') {
        var len = Math.floor(hex.length / 2);
        var ret = new Uint8Array(len);
        for (var i = 0; i < len; i++) {
            ret[i] = parseInt(hex.substr(i * 2, 2), 16);
        }
        return ret;
    }
}

function hasPlugin(mime) {
    if(navigator.mimeTypes && mime in navigator.mimeTypes) {
        return true;
    }
    return false;
}

function loadBcyPlugin() {
    var mime = 'application/x-cryptolib05plugin';
    var element = "bcy" + mime.replace('/', '').replace('-', '');
    if(document.getElementById(element)) {
        return document.getElementById(element);
    }
    var objectTag = '<object id="' + element + '" type="' + mime + '" style="width: 1px; height: 1px; position: absolute; visibility: hidden;"></object>';
    var div = document.createElement("div");
    div.setAttribute("id", 'plugin' + element);
    document.body.appendChild(div);
    document.getElementById('plugin' + element).innerHTML = objectTag;
    return document.getElementById(element);
}

function signBcy(signer) {
    var plugin = loadBcyPlugin();
    if (plugin.valid) {
        var code = plugin.Sign(hex2Array(signer.options.hash.hex));
        if (code === 0 || code === 7) {
            var sign = plugin.Signature;
            signer.options.signature.value = sign;
            if (signer.options.afterSign) {
                signer.options.afterSign(signer, signer.options.signature);
            }
        }
        else {
            if (signer.options.onError) {
                signer.options.onError(signer, 'sign() failed');
            }
        }
    }
}

if (window.hwcrypto) {
    window.hwcrypto.use('auto');
    window.hwcrypto.debug().then(function(response) {
      console.log('Debug: ' + response);
    }, function(err) {
      console.log('debug() failed: ' + err);
      return;
    });
}

function signHwCrypto(signer) {
    window.hwcrypto.getCertificate({lang: 'en'}).then(function(certificate) {
        window.hwcrypto.sign(certificate, {type: signer.options.hash.type, hex: signer.options.hash.hex}, {lang: 'en'}).then(function(signature) {
            signer.options.signature.certificate = certificate.hex;
            signer.options.signature.value = signature.hex;
            if (signer.options.afterSign) {
                signer.options.afterSign(signer, signer.options.signature);
            }
        }, function(err) {
            if (signer.options.onError) {
                signer.options.onError(signer, err);
            }
            console.log("sign() failed: " + err);
        });
    }, function(err) {
        console.log("getCertificate() failed: " + err);
        if (signer.options.onError) {
            signer.options.onError(signer, err);
        }
    });
}

$.signer = $.signer || {};
$.extend($.signer, {
    options: {
        hash: {
            type: 'sha256',
            hex: false,
            value: false
        },
        signature: {
            certificate: false,
            value: false
        },
        document: false,
        beforeSign: false,
        afterSign: false,
        onError: false
    },
    sign: function(options) {
        var signer = this;
        $.extend(signer.options, options);
        if (signer.options.beforeSign) {
            signer.options.beforeSign(signer, signer.options.hash);
        }

        if (hasPlugin('application/x-cryptolib05plugin')) {
            signBcy(signer);
        }
        else if (window.hwcrypto) {
            signHwCrypto(signer);
        }
        return signer;
    }
});

$.extend({
    getCertificate: function(){
        var cert = null;
        if (window.hwcrypto) {
            window.hwcrypto.getCertificate({lang: 'en'}).then(function(response) {
                cert = response.hex;
            }, function(err) {
                console.log("getCertificate() failed: " + err);
            });
        }
        return cert;
    },
    sign: function(options) {
        return $.signer.sign(options);
    }
});

})(jQuery);
