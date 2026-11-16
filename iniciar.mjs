import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'web','dist');
const types={'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.svg':'image/svg+xml','.png':'image/png'};
const server=http.createServer((req,res)=>{
 try {
  if(req.method!=='GET'&&req.method!=='HEAD'){res.writeHead(405);res.end();return;}
  const url=new URL(req.url,'http://127.0.0.1');
  const relative=decodeURIComponent(url.pathname);
  const target=path.resolve(root,'.'+(relative==='/'?'/index.html':relative));
  if(!target.startsWith(root+path.sep)){res.writeHead(403);res.end();return;}
  if(!fs.existsSync(target)||!fs.statSync(target).isFile()){res.writeHead(404);res.end('Archivo no encontrado');return;}
  res.writeHead(200,{'Content-Type':types[path.extname(target)]||'application/octet-stream','Cache-Control':'no-cache','X-Content-Type-Options':'nosniff'});
  if(req.method==='HEAD')res.end();else fs.createReadStream(target).pipe(res);
 }catch{res.writeHead(400);res.end('Solicitud no válida');}
});
server.on('error',e=>{console.error(e.code==='EADDRINUSE'?'El puerto 3000 está ocupado. Si ACTIUM ya está abierto, visita http://localhost:3000/':e.message);process.exitCode=1});
server.listen(3000,'localhost',()=>console.log('\nACTIUM v2 está listo.\n\nAbre en tu navegador: http://localhost:3000/\n\nMantén esta ventana abierta. Ctrl+C para cerrar.\nUsa siempre esta dirección para acceder a los mismos registros locales.\n'));
