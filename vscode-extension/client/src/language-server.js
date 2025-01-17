import { join } from "node:path";
import { window, workspace } from "vscode";
import { LanguageClient, LanguageClientOptions, ServerOptions } from "vscode-languageclient/node";
import { SimpleLogger } from "./simple-logger";

/**
 * @param {string} javaBin
 */
export async function activateLanguageServer(javaBin) {
    const logChannel = window.createOutputChannel('ZenScript Language Server', "log");
    const logger = new SimpleLogger(logChannel);
    logger.info('Starting ZensScript language server');

    const config = workspace.getConfiguration();
    const classpath = join(__dirname, '..', '..', 'server', '*');
    const main = 'raylras.zen.lsp.StandardIOLauncher';

    /** @type {string[]} */
    const args = [];

    args.push('-classpath', classpath);

    let suspend = false;
    if (config.get('zenscript.languageServer.debug')) {
        logger.info(`Language server is running in debug mode`);
        if (config.get('zenscript.languageServer.suspend')) {
            args.push('-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005,quiet=y');
            suspend = true;
        } else {
            args.push('-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005,quiet=y');
        }
    }

    args.push(main);

    logger.info(`Command: "${[javaBin, ...args].join(' ')}"`);
    if (suspend) {
        logger.info('Waiting for debugger to connect...');
    }

    /** @type {ServerOptions} */
    const serverOptions = {
        command: javaBin,
        args: args,
        options: {}
    };

    /** @type {LanguageClientOptions} */
    const clientOptions = {
        documentSelector: [{ scheme: 'file', language: 'zenscript' }],
        synchronize: { configurationSection: ['zenscript'] },
        outputChannel: logChannel
    };

    const client = new LanguageClient('ZenScript Language Client', serverOptions, clientOptions);
    await client.start();
    return client;
}
