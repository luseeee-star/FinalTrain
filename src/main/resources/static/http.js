;(function () {
  const BASE_URL = 'http://localhost:8080';
  const LOGIN_PAGE = 'login.html';
  const PUBLIC_PAGES = ['login.html', 'register.html'];

  if (typeof axios === 'undefined') {
    throw new Error('axios is not loaded');
  }

  function getCurrentPageName() {
    const path = (window.location && window.location.pathname) || '';
    const page = path.substring(path.lastIndexOf('/') + 1);
    return page || LOGIN_PAGE;
  }

  function parseJwtPayload(token) {
    if (!token) return null;
    const parts = token.split('.');
    if (parts.length < 2) return null;
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64 + '='.repeat((4 - base64.length % 4) % 4);
    try {
      return JSON.parse(atob(padded));
    } catch (e) {
      return null;
    }
  }

  function isTokenValid(token) {
    const payload = parseJwtPayload(token);
    if (!payload) return false;
    if (payload.exp && Date.now() >= payload.exp * 1000) return false;
    return true;
  }

  function ensurePageAuth() {
    const page = getCurrentPageName().toLowerCase();
    if (PUBLIC_PAGES.indexOf(page) !== -1) return true;
    const token = sessionStorage.getItem('token');
    if (!isTokenValid(token)) {
      sessionStorage.removeItem('token');
      window.location.href = LOGIN_PAGE;
      return false;
    }
    return true;
  }

  ensurePageAuth();

  const http = axios.create({
    baseURL: BASE_URL,
    timeout: 10000
  });
//http.interceptors.request.use：拦截从后端返回给前端的那个 Response
  http.interceptors.request.use(
    function (config) {
      //取出token
      const token = sessionStorage.getItem('token');
      if (isTokenValid(token)) {
        config.headers = config.headers || {};
        config.headers.Authorization = token;
      } else if (token) {
        sessionStorage.removeItem('token');
      }
      return config;
    },
    function (error) {
      return Promise.reject(error);
    }
  );

  //全局拦截器，如果后端发现 Token 伪造或过期，返回了 401，前端会自动清空本地存储并强制跳转到登录页。
  http.interceptors.response.use(
    function (response) {
      return response;
    },
    function (error) {
      const status = error && error.response ? error.response.status : 0;
      if (status === 401) {
        sessionStorage.removeItem('token');
        alert('登录已过期，请重新登录');
        if (!location.href.endsWith('/' + LOGIN_PAGE) && !location.href.endsWith(LOGIN_PAGE)) {
          window.location.href = LOGIN_PAGE;
        }
      } else if (!error.response) {
        alert('网络异常，请检查后端服务是否启动');
      }
      return Promise.reject(error);
    }
  );

  // window 是浏览器的全局对象。
// 把 httpClient 挂载到 window 上，相当于定义了一个“全局单例工具类”，在任何页面都能直接用。
  window.httpClient = {
    ensurePageAuth: ensurePageAuth,
    parseJwtPayload: parseJwtPayload,
    request: function (url, method, data, config) {
      //参数：
      //Object.assign 就像是把两个 Map 合并。
      // 左边是必填项（url, method, data），右边是可选的扩展配置（config）。
      // config || {} 是个保底：如果你没传第四个参数，就给它一个空对象，防止程序报错。
      //方法：
      //调用我们在上面定义好的 axios 实例 (http)
      // .then(function (resp) { return resp.data; })
      // 这步最关键：后端返回的是整个 Response（含状态码、Header 等），
      // 这里直接帮你“拆包”，只把 body 里的数据返回给业务页面(前端)。
      return http.request(Object.assign({
        url: url,
        method: method,
        data: data  //请求体，后端的@RequestBody接受
      }, config || {})).then(function (resp) { return resp.data; });
    },
    /**
     * 专门处理文件上传的方法
     * 类比后端：处理 MultipartFile
     */
    upload: function (url, formData, config) {
      return http.request(Object.assign({
        url: url,
        method: 'POST',
        data: formData
      }, config || {})).then(function (resp) { return resp.data; });
    }
  };
})();

