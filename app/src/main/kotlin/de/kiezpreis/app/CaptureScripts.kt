package de.kiezpreis.app

/**
 * JavaScript run inside the retailer page. Both scripts only read what the page already has;
 * they never click, submit, solve challenges or send anything anywhere.
 */
object CaptureScripts {

    /**
     * Installed at document start: keeps a copy of the JSON the page itself loads through fetch/XHR,
     * so offers rendered from such responses can be read later. Bounded in count and size.
     */
    val RECORDER = """
        (function () {
          if (window.__kiezRecorder) return;
          var store = window.__kiezRecorder = { responses: [] };
          function keep(url, contentType, text) {
            try {
              if (!text || text.length > 3000000) return;
              if (String(contentType || '').indexOf('json') < 0) return;
              var t = String(text).trim();
              if (t[0] !== '{' && t[0] !== '[') return;
              if (store.responses.length >= 60) store.responses.shift();
              store.responses.push({ url: String(url || '').slice(0, 400), body: t });
            } catch (e) {}
          }
          var originalFetch = window.fetch;
          if (originalFetch) {
            window.fetch = function () {
              return originalFetch.apply(this, arguments).then(function (response) {
                try {
                  var ct = response.headers.get('content-type');
                  if (ct && ct.indexOf('json') >= 0) {
                    response.clone().text().then(function (t) { keep(response.url, ct, t); }, function () {});
                  }
                } catch (e) {}
                return response;
              });
            };
          }
          var open = XMLHttpRequest.prototype.open;
          var send = XMLHttpRequest.prototype.send;
          XMLHttpRequest.prototype.open = function (method, url) {
            this.__kiezUrl = url;
            return open.apply(this, arguments);
          };
          XMLHttpRequest.prototype.send = function () {
            var xhr = this;
            xhr.addEventListener('load', function () {
              try {
                if (xhr.responseType === '' || xhr.responseType === 'text') {
                  keep(xhr.__kiezUrl, xhr.getResponseHeader('content-type'), xhr.responseText);
                }
              } catch (e) {}
            });
            return send.apply(this, arguments);
          };
        })();
    """.trimIndent()

    /**
     * Run when the user taps "Angebote übernehmen". Returns one JSON string matching
     * `CapturedPage`. Product tiles are only reported when exactly one current price sits in them.
     */
    val COLLECT = """
        (function () {
          function text(el) { return el ? String(el.textContent || '').replace(/\s+/g, ' ').trim() : ''; }
          var PRICE = /(\d{1,3}|[-–])\s?[.,]\s?(\d{2}|[-–]{1,2})(?!\d)/;
          var OLD = /(old|strike|regular|before|former|uvp|statt|crossed|was-price|original)/i;
          var BASE = /(base|unit|grammage|grundpreis|per-unit|perunit|comparison)/i;
          var SIZE = /(amount|quantity|size|packag|weight|content|subtitle|grammage)/i;
          var TITLE = 'h1,h2,h3,h4,h5,[class*="title" i],[class*="name" i],[itemprop="name"],[data-testid*="title" i]';
          var out = {
            url: location.href, title: document.title,
            jsonLd: [], embeddedJson: [],
            responses: (window.__kiezRecorder && window.__kiezRecorder.responses) || [],
            domCards: []
          };
          document.querySelectorAll('script[type="application/ld+json"]').forEach(function (s) {
            if (s.textContent.length < 3000000) out.jsonLd.push(s.textContent);
          });
          document.querySelectorAll('script[type="application/json"], script#__NEXT_DATA__').forEach(function (s) {
            if (s.textContent.length < 6000000) out.embeddedJson.push(s.textContent);
          });

          function cls(el) { return String(el.className && el.className.baseVal !== undefined ? el.className.baseVal : el.className || ''); }
          function isOld(el) {
            for (var e = el, i = 0; e && i < 3; e = e.parentElement, i++) {
              if (e.tagName === 'DEL' || e.tagName === 'S' || OLD.test(cls(e))) return true;
            }
            return false;
          }
          var seen = new Set();
          var nodes = document.querySelectorAll('[class*="price" i],[data-testid*="price" i],[itemprop="price"]');
          for (var n = 0; n < nodes.length && out.domCards.length < 400; n++) {
            var p = nodes[n];
            if (isOld(p) || BASE.test(cls(p)) || !PRICE.test(text(p))) continue;
            var card = null;
            for (var a = p.parentElement, depth = 0; a && depth < 8; a = a.parentElement, depth++) {
              if (a.querySelector(TITLE)) { card = a; break; }
            }
            if (!card || seen.has(card)) continue;
            seen.add(card);
            var current = new Set(), old = null, base = null, size = null;
            card.querySelectorAll('[class*="price" i],[data-testid*="price" i],[itemprop="price"],del,s').forEach(function (el) {
              var t = text(el);
              if (!PRICE.test(t)) return;
              if (BASE.test(cls(el)) || /=|\/\s?(kg|l|100)/i.test(t)) { base = base || t; return; }
              if (isOld(el)) { old = old || t; return; }
              if (el.querySelector('[class*="price" i]')) return; // a wrapper, its children are counted
              current.add(t.match(PRICE)[0].replace(/\s/g, ''));
            });
            if (current.size !== 1) continue;
            card.querySelectorAll('*').forEach(function (el) {
              if (!size && el.children.length === 0 && SIZE.test(cls(el)) && /\d/.test(text(el)) && text(el).length < 60) size = text(el);
            });
            var titleEl = card.querySelector(TITLE);
            var img = card.querySelector('img');
            out.domCards.push({
              title: text(titleEl).slice(0, 200),
              price: Array.from(current)[0],
              oldPrice: old, basePrice: base, size: size,
              note: text(card).slice(0, 300),
              image: img && img.src && img.src.indexOf('https://') === 0 ? img.src : null
            });
          }
          return JSON.stringify(out);
        })();
    """.trimIndent()
}
